package org.jenkinsci.plugins.deploydb;

import com.github.lookout.whoas.AbstractHookQueue;
import com.github.lookout.whoas.AbstractHookRunner;
import com.github.lookout.whoas.HookRequest;
import com.github.lookout.whoas.WhoasFactory;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.listeners.RunListener;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.deploydb.model.ReportWebhook;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.jenkinsci.plugins.deploydb.BuildCompletionListener.MAX_DELIVERY_ATTEMPTS;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildCompletionListenerTest {

    /** Default base URL to configure during test cases. */
    private static final String REPORTING_BASE_URL = "https://ddb.example.com:8443";

    /** Default name to use when generating new jobs. */
    private static final String JOB_NAME = "foobar-service";

    /** Deployment ID value to use for incoming webhooks. */
    private static final long HOOK_DEPLOYMENT_ID = 123;

    @Rule public final JenkinsRule jenkins = new JenkinsRule();

    private Handler logHandler;
    private ArgumentCaptor<LogRecord> logCaptor;

    @Before public void setUp() {
        // Hook into the logger, so we can assert on logging events
        logHandler = mock(Handler.class);
        logCaptor = ArgumentCaptor.forClass(LogRecord.class);
        BuildCompletionListener.LOGGER.addHandler(logHandler);
    }

    @After public void tearDown() {
        BuildCompletionListener.LOGGER.removeHandler(logHandler);
    }

    @Test public void regularBuildShouldNotSendReportWebhook() throws Exception {
        // Given that the DeployDB plugin has been configured
        final AbstractHookQueue webhookQueue = createWebhookQueue();
        setUpBuildCompletionListener(webhookQueue);

        // When a build is executed, which was not triggered by DeployDB
        FreeStyleProject job = jenkins.createFreeStyleProject();
        jenkins.buildAndAssertSuccess(job);

        // Then no webhooks should have been sent
        assertWebhookDeliveryAttempts(webhookQueue, 0);
    }

    @Test public void deployDbTriggeredBuildShouldSendReportWebhook() throws Exception {
        // Given that the DeployDB plugin has been configured
        final AbstractHookQueue webhookQueue = createWebhookQueue();
        setUpBuildCompletionListener(webhookQueue);

        // When a build is executed, which was triggered by DeployDB
        triggerDeployDbBuildAndAssertSuccess();

        // Then a webhook should have been sent
        assertWebhookDeliveryAttempts(webhookQueue, 1);
    }

    @Test public void deployDbTriggeredBuildWithSilentModeShouldNotSendReportWebhook() throws Exception {
        // Given that the DeployDB plugin has been configured
        final AbstractHookQueue webhookQueue = createWebhookQueue();
        setUpBuildCompletionListener(webhookQueue);

        // And there is a job configured with silent mode enabled
        FreeStyleProject job = jenkins.createFreeStyleProject(JOB_NAME);
        DeployDbTrigger trigger = new DeployDbTrigger();
        trigger.setSilentMode(true);
        job.addTrigger(trigger);

        // When a build of that job is triggered by DeployDB
        Future<FreeStyleBuild> build = job.scheduleBuild2(0, new Cause.UserIdCause(), createTriggerAction());
        jenkins.assertBuildStatusSuccess(build);

        // Then a webhook should not have been sent
        assertWebhookDeliveryAttempts(webhookQueue, 0);
    }

    @Test public void successfulDeployDbTriggeredBuildShouldReportSuccess() throws Exception {
        triggerBuildAndAssertReportWebhookValues(Result.SUCCESS, ReportWebhook.Status.SUCCESS);
    }

    @Test public void unstableDeployDbTriggeredBuildShouldReportFailure() throws Exception {
        triggerBuildAndAssertReportWebhookValues(Result.UNSTABLE, ReportWebhook.Status.FAILURE);
    }

    @Test public void failedDeployDbTriggeredBuildShouldReportFailure() throws Exception {
        triggerBuildAndAssertReportWebhookValues(Result.FAILURE, ReportWebhook.Status.FAILURE);
    }

    @Test public void notBuiltDeployDbTriggeredBuildShouldReportFailure() throws Exception {
        triggerBuildAndAssertReportWebhookValues(Result.NOT_BUILT, ReportWebhook.Status.FAILURE);
    }

    @Test public void abortedDeployDbTriggeredBuildShouldReportFailure() throws Exception {
        triggerBuildAndAssertReportWebhookValues(Result.ABORTED, ReportWebhook.Status.FAILURE);
    }

    private void triggerBuildAndAssertReportWebhookValues(Result buildResult, ReportWebhook.Status expectedHookStatus)
            throws Exception {
        // Given there has been a build triggered by DeployDB, which returns a given build result
        AbstractBuild<?, ?> build = triggerDeployDbBuildAndAssertResult(buildResult);

        // When we create the reporting webhook for this build
        ReportWebhook reportWebhook = BuildCompletionListener.buildReportWebhook(build);

        // Then it should be contain the job/build properties, including the expected success/failure status
        assertThat(reportWebhook.getName(), is(JOB_NAME));
        assertThat(reportWebhook.getStatus(), is(expectedHookStatus));
        assertThat(reportWebhook.getInfoUrl(), startsWith(jenkins.getInstance().getRootUrl()));
        assertThat(reportWebhook.getInfoUrl(), endsWith(String.format("job/%s/1/", JOB_NAME))); // Build #1
    }

    @Test public void deployDbTriggeredBuildShouldNotSendReportWebhookForNullBaseUrl() throws Exception {
        assertWebhookNotSentForInvalidBaseUrl(null);
    }

    @Test public void deployDbTriggeredBuildShouldNotSendReportWebhookForEmptyBaseUrl() throws Exception {
        assertWebhookNotSentForInvalidBaseUrl(" ");
    }

    @Test public void deployDbTriggeredBuildShouldNotSendReportWebhookForWeirdBaseUrl() throws Exception {
        assertWebhookNotSentForInvalidBaseUrl("ftp://hooks.local/deploydb");
    }

    private void assertWebhookNotSentForInvalidBaseUrl(String invalidBaseUrl) throws Exception {
        // Given that the DeployDB plugin has been configured with some sort of invalid base URL
        final AbstractHookQueue webhookQueue = createWebhookQueue();
        setUpBuildCompletionListener(webhookQueue, invalidBaseUrl);

        // When a build is executed, which was triggered by DeployDB
        triggerDeployDbBuildAndAssertSuccess();

        // Then no webhooks should have been sent
        assertWebhookDeliveryAttempts(webhookQueue, 0);
    }

    @Test public void deployDbTriggeredBuildShouldRetrySendingWebhook() throws Exception {
        // Given that the DeployDB plugin has been configured with a queue that sometimes fails to accept messages
        final AbstractHookQueue webhookQueue = createWebhookQueue(false, true, true);
        setUpBuildCompletionListener(webhookQueue);

        // When a build is executed, which was triggered by DeployDB
        triggerDeployDbBuildAndAssertSuccess();

        // Then we should have attempted multiple times until the hook was queued
        assertWebhookDeliveryAttempts(webhookQueue, 2);
    }

    @Test public void deployDbTriggeredBuildShouldEventuallyGiveUpSendingWebhook() throws Exception {
        // Given that the DeployDB plugin has been configured with a queue that always fails to accept messages
        final AbstractHookQueue webhookQueue = createWebhookQueue(false, false, false, false);
        setUpBuildCompletionListener(webhookQueue);

        // When a build is executed, which was triggered by DeployDB
        triggerDeployDbBuildAndAssertSuccess();

        // Then we should have attempted several times to deliver, but no more than the limit
        assertWebhookDeliveryAttempts(webhookQueue, MAX_DELIVERY_ATTEMPTS);

        // And a warning should have been written to the log
        verify(logHandler).publish(logCaptor.capture());
        assertThat(logCaptor.getValue().getLevel(), is(Level.WARNING));
    }

    // Helper methods

    private AbstractBuild<?, ?> triggerDeployDbBuildAndAssertSuccess() throws Exception {
        return triggerDeployDbBuildAndAssertResult(Result.SUCCESS);
    }

    /**
     * Creates a job, executes a build of that, as if triggered by DeployDB, and ensures the build has a given outcome.
     *
     * @param result The desired build result.
     * @return The build itself.
     */
    private AbstractBuild<?, ?> triggerDeployDbBuildAndAssertResult(Result result) throws Exception {
        // Create an incoming DeployDB hook with a fixed deployment ID
        DeployDbBuildAction action = createTriggerAction();

        // Create a job, set the desired result for every build
        FreeStyleProject job = jenkins.createFreeStyleProject(JOB_NAME);
        job.getBuildersList().add(new MockBuilder(result));

        // Schedule a build, and assert that it completed as expected
        Future<FreeStyleBuild> build = job.scheduleBuild2(0, new Cause.UserIdCause(), action);
        return jenkins.assertBuildStatus(result, build.get());
    }

    /** Verifies that there were the given number of attempts to push a webhook into the queue. */
    private static void assertWebhookDeliveryAttempts(AbstractHookQueue queue, int expectedDeliveryAttempts) {
        verify(queue, times(expectedDeliveryAttempts)).push(any(HookRequest.class));
    }

    /** @return A build action containing a trigger webhook which has a fixed deployment ID. */
    private static DeployDbBuildAction createTriggerAction() {
        TriggerWebhook hook = mock(TriggerWebhook.class);
        when(hook.getId()).thenReturn(HOOK_DEPLOYMENT_ID);
        return new DeployDbBuildAction(hook);
    }

    /** @return A mock webhook queue which always accepts hooks added to it. */
    private static AbstractHookQueue createWebhookQueue() {
        return createWebhookQueue(true);
    }

    /**
     * @param initialResponse Whether the initial attempt to add to the queue should succeed.
     * @param furtherResponses Whether subsequent attempts should succeed.
     * @return A mock webhook queue which accepts hooks as configured.
     */
    private static AbstractHookQueue createWebhookQueue(boolean initialResponse, Boolean... furtherResponses) {
        AbstractHookQueue queue = mock(AbstractHookQueue.class);
        when(queue.push(any(HookRequest.class))).thenReturn(initialResponse, furtherResponses);
        return queue;
    }

    private static BuildCompletionListener setUpBuildCompletionListener(AbstractHookQueue webhookQueue) {
        return setUpBuildCompletionListener(webhookQueue, REPORTING_BASE_URL);
    }

    private static BuildCompletionListener setUpBuildCompletionListener(AbstractHookQueue webhookQueue, String baseUrl) {
        // Ensure that a base URL has been configured so hooks can be delivered
        DeployDbConfig ddbConfig = GlobalConfiguration.all().get(DeployDbConfig.class);
        ddbConfig.setBaseUrl(baseUrl);

        // Set up the listener with an observable webhook queue
        BuildCompletionListener listener = RunListener.all().get(BuildCompletionListener.class);
        listener.setWhoasFactory(new NoopWhoasFactory(webhookQueue));
        return listener;
    }

    /** WhoasFactory which won't deliver any webhooks pushed to its queue. */
    private static final class NoopWhoasFactory extends WhoasFactory {

        private AbstractHookQueue queue;

        private NoopWhoasFactory(AbstractHookQueue queue) {
            this.queue = queue;
        }

        @Override
        public AbstractHookQueue buildQueue() {
            return queue;
        }

        @Override
        public AbstractHookRunner buildRunner(AbstractHookQueue hookQueue) {
            return mock(AbstractHookRunner.class);
        }

    }

}