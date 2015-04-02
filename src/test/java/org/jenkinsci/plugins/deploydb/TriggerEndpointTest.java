package org.jenkinsci.plugins.deploydb;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.WebResponse;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TriggerEndpointTest {

    /** Relative URL path from Jenkins root to the {@link TriggerEndpoint}. */
    private static final String ENDPOINT = "deploydb/trigger";

    @Rule public final JenkinsRule jenkins = new JenkinsRule();

    private JenkinsRule.WebClient webClient;

    @Before
    public void setUp() throws IOException {
        // Set up a web client so we can submit HTTP requests to the endpoint
        webClient = jenkins.createWebClient();

        // Disable job execution, so we can examine whether jobs are being queued
        Jenkins j = jenkins.getInstance();
        j.setNumExecutors(0);
        j.setNodes(j.getNodes());
    }

    @Test public void httpGetRequestIsRejected() throws Exception {
        // Sending anything other than a POST request to the endpoint should return an HTTP 405 error
        webClient.assertFails(ENDPOINT, HTTP_BAD_METHOD);
    }

    @Test public void nonJsonHttpPostRequestIsRejected() throws IOException {
        try {
            submitWebhookRequest("hook_malformed.json");
            fail("Should have thrown 400 error");
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(HTTP_BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test public void hookWithMissingMimeTypeIsRejected() throws IOException {
        assertWebhookRequestWithBadMimeTypeIsRejected(null);
    }

    @Test public void hookWithBadMimeTypeIsRejected() throws IOException {
        assertWebhookRequestWithBadMimeTypeIsRejected("application/json");
    }

    @Test public void unmatchedHookShouldTriggerNoBuilds() throws IOException {
        // When an empty JSON webhook is posted
        WebResponse response = submitWebhookRequest("hook_empty.json");

        // Then an HTTP 200 response should have been returned
        assertEquals(HTTP_OK, response.getStatusCode());

        // And no builds should have been scheduled
        assertNoJobsTriggered(response);
    }

    @Test public void hookShouldTriggerNoBuilds() throws Exception {
        // Given we have a job configured
        jenkins.createFreeStyleProject("a");

        // When a JSON webhook for a service deployment is posted
        WebResponse response = submitWebhookRequest("hook_trigger_deployment_started.json");

        // Then an HTTP 200 response should have been returned
        assertEquals(HTTP_OK, response.getStatusCode());

        // And no builds should have been scheduled
        assertNoJobsTriggered(response);
    }

    @Test public void hookShouldTriggerSingleMatchingJob() throws Exception {
        // Given we have a job configured with the DeployDB trigger
        FreeStyleProject jobA = configureDeployDbTriggeredJob("a", true);

        // When a JSON webhook for a service deployment is posted
        WebResponse response = submitWebhookRequest("hook_trigger_deployment_started.json");

        // Then a build of that job should have been scheduled
        assertJobsTriggered(response, jobA);

        // And DeployDB should be attributed as the cause
        Queue.Item build = jenkins.getInstance().getQueue().getItem(jobA);
        assertEquals(1, Util.filter(build.getCauses(), DeployDbCause.class).size());
    }

    @Test public void hookShouldTriggerMultipleMatchingJobs() throws Exception {
        // Given we have multiple jobs configured, with and without the DeployDB trigger
        FreeStyleProject jobA = configureDeployDbTriggeredJob("a", true);
        FreeStyleProject jobB = configureDeployDbTriggeredJob("b", false);
        FreeStyleProject jobC = configureDeployDbTriggeredJob("c", true);
        FreeStyleProject jobD = jenkins.createFreeStyleProject("d");

        // When a JSON webhook for a service deployment is posted
        WebResponse response = submitWebhookRequest("hook_trigger_deployment_started.json");

        // Then builds of only the matching jobs should have been scheduled
        assertJobsTriggered(response, jobA, jobC);
    }

    @Test public void hookShouldNotTriggerDisabledMatchingJob() throws Exception {
        // Given we have multiple jobs configured, all of which could be triggered by an incoming webhook
        FreeStyleProject jobA = configureDeployDbTriggeredJob("a", true);
        FreeStyleProject jobB = configureDeployDbTriggeredJob("b", true);
        FreeStyleProject jobC = configureDeployDbTriggeredJob("c", true);

        // And a job has been disabled
        jobA.disable();

        // When a JSON webhook for a service deployment is posted
        WebResponse response = submitWebhookRequest("hook_trigger_deployment_started.json");

        // Then builds of only the enabled jobs should have been scheduled
        assertJobsTriggered(response, jobB, jobC);
    }

    /** Sends a JSON webhook payload with the given Content-Type header value and asserts its rejection. */
    private void assertWebhookRequestWithBadMimeTypeIsRejected(String mimeType) throws IOException {
        try {
            // When a valid JSON payload is posted, but with an unrecognised MIME type
            submitWebhookRequest("hook_empty.json", mimeType);
        } catch (FailingHttpStatusCodeException e) {
            // Then the endpoint should reject the request
            assertEquals(HTTP_UNSUPPORTED_TYPE, e.getStatusCode());
        }
    }

    /** Asserts that no builds have been enqueued. */
    private void assertNoJobsTriggered(WebResponse response) {
        assertJobsTriggered(response);
    }

    /**
     * Asserts that exactly one build of the given jobs (and no other jobs) has been enqueued due to a webhook request.
     *
     * @param response The HTTP response to the incoming webhook.
     * @param jobs Zero or more jobs to confirm as being the only jobs scheduled.
     */
    private void assertJobsTriggered(WebResponse response, Queue.Task... jobs) {
        final int count = jobs.length;

        assertEquals(count, jenkins.getInstance().getQueue().getItems().length);
        for (Queue.Task job : jobs) {
            assertTrue(jenkins.getInstance().getQueue().contains(job));
        }

        assertEquals(Messages.TriggeredBuilds(count), response.getContentAsString().trim());
    }

    /**
     * Creates a job, configured with a DeployDB trigger.
     *
     * @param shouldMatchHooks {@code true} iff this job should always be triggered by incoming DeployDB hooks.
     */
    private FreeStyleProject configureDeployDbTriggeredJob(String name, boolean shouldMatchHooks) throws Exception {
        DeployDbTrigger trigger = mock(DeployDbTrigger.class);
        when(trigger.accepts(any(AbstractProject.class), any(TriggerWebhook.class))).thenReturn(shouldMatchHooks);

        FreeStyleProject job = jenkins.createFreeStyleProject(name);
        job.addTrigger(trigger);
        return job;
    }

    private WebResponse submitWebhookRequest(String filename) throws IOException {
        return submitWebhookRequest(filename, EventType.DEPLOYMENT_CREATED.getMimeType());
    }

    /**
     * Submits the contents of the given file as a webhook request to the {@link TriggerEndpoint}.
     *
     * @param filename Name of a file in the resources directory for this class.
     * @param contentType The value of the Content-Type header to send with the webhook request, if not {@code null}.
     * @return The HTTP response to the webhook sent.
     */
    private WebResponse submitWebhookRequest(String filename, String contentType) throws IOException {
        WebRequestSettings req = new WebRequestSettings(webClient.createCrumbedUrl(ENDPOINT), POST);
        if (contentType != null) {
            req.setAdditionalHeader("Content-Type", contentType);
        }
        req.setRequestBody(IOUtils.toString(getClass().getResourceAsStream(filename), Charsets.UTF_8));
        return webClient.getPage(req).getWebResponse();
    }

}