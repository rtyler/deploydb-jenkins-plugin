package org.jenkinsci.plugins.deploydb.model.events;

import hudson.model.AbstractProject;
import org.junit.Before;
import org.junit.Test;

import static java.util.Locale.ROOT;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.deploydb.Util.createWebhook;
import static org.jenkinsci.plugins.deploydb.model.EventType.DEPLOYMENT_COMPLETED;
import static org.jenkinsci.plugins.deploydb.model.EventType.DEPLOYMENT_STARTED;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class DeployDbTriggerEventTest {

    private AbstractProject<?, ?> job;

    @Before
    public void setUp() throws Exception {
        job = mock(AbstractProject.class);
    }

    @Test public void triggerEventWithNullRegexShouldNotMatch() throws Exception {
        // Given an event definition, which has never been configured
        DeployDbTriggerEvent event = new DeploymentStartedEvent();
        event.setServiceNameRegex(null);

        // Then it should not match against any webhooks
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED)), is(false));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_COMPLETED)), is(false));
    }

    @Test public void triggerEventWithEmptyRegexShouldNotMatch() throws Exception {
        // Given an event definition, which has been configured, but with an empty regular expression
        DeployDbTriggerEvent event = new DeploymentStartedEvent();
        event.setServiceNameRegex("");

        // Then it should not match against any webhooks
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED)), is(false));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_COMPLETED)), is(false));
    }

    @Test public void triggerEventWithInvalidRegexShouldNotMatch() throws Exception {
        // Given an event definition, which has been configured, but with a regular expression that does not compile
        DeployDbTriggerEvent event = new DeploymentStartedEvent();
        event.setServiceNameRegex("foo(bar");

        // Then it should not match against any webhooks
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED)), is(false));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_COMPLETED)), is(false));
    }

    @Test public void triggerEventShouldMatchBasedOnServiceName() throws Exception {
        // Given a deployment started event definition, configured for a certain service
        DeployDbTriggerEvent event = new DeploymentStartedEvent();
        event.setServiceNameRegex("foo-service");

        // Then it should not match against a hook for a different service
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "bar-service")), is(false));

        // And it should match against a hook with the configured service
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "foo-service")), is(true));
    }

    @Test public void triggerEventShouldMatchBasedOnHookEventType() throws Exception {
        final String serviceName = "some-service";

        // Given a deployment started event definition
        DeployDbTriggerEvent event = new DeploymentStartedEvent();
        event.setServiceNameRegex(serviceName);

        // Then it should not match against a hook of a different type
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_COMPLETED, serviceName)), is(false));

        // And it should match against a hook of the correct type
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, serviceName)), is(true));

        // And the service name match should be case-insensitive
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, serviceName.toUpperCase(ROOT))), is(true));
    }

    @Test public void triggerEventShouldMatchBasedOnServiceRegularExpression() throws Exception {
        // Given a deployment started event definition, with a regular expression value
        DeployDbTriggerEvent event = new DeploymentStartedEvent();
        event.setServiceNameRegex("foo(ba[rz])?-\\d{2,4}");

        // Then it should only accept hooks whose service name match the regular expression
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "foo-00")), is(true));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "foobar-123")), is(true));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, " foobar-123 ")), is(true));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "foobaz-9876")), is(true));

        // And other hooks should not match
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "bar")), is(false));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "foobar")), is(false));
        assertThat(event.accepts(job, createWebhook(DEPLOYMENT_STARTED, "foobar-0")), is(false));
    }

}