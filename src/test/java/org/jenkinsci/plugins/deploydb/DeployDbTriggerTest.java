package org.jenkinsci.plugins.deploydb;

import hudson.model.AbstractProject;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.jenkinsci.plugins.deploydb.model.events.DeployDbTriggerEvent;
import org.jenkinsci.plugins.deploydb.model.events.DeploymentCompletedEvent;
import org.jenkinsci.plugins.deploydb.model.events.DeploymentCreatedEvent;
import org.jenkinsci.plugins.deploydb.model.events.DeploymentStartedEvent;
import org.jenkinsci.plugins.deploydb.model.events.PromotionCompletedEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.deploydb.Util.getWebhook;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployDbTriggerTest {

    private AbstractProject<?, ?> job;
    private TriggerWebhook hook;

    @Before
    public void setUp() throws Exception {
        job = mock(AbstractProject.class);
        hook = getWebhook("hook_trigger_deployment_started.json");
    }

    @Test public void triggerWithNullEventsListShouldNotMatch() throws Exception {
        // Given a trigger definition, which has never been configured
        DeployDbTrigger trigger = new DeployDbTrigger();
        trigger.setTriggerEventTypes(null);

        // Then it should not match against any webhooks
        assertThat(trigger.accepts(job, hook), is(false));
    }

    @Test public void triggerWithEmptyEventsShouldNotMatch() throws Exception {
        // Given a trigger definition, which has been configured with no event types
        DeployDbTrigger trigger = new DeployDbTrigger();
        trigger.setTriggerEventTypes(Collections.<DeployDbTriggerEvent>emptyList());

        // Then it should not match against any webhooks
        assertThat(trigger.accepts(job, hook), is(false));
    }

    @Test public void triggerWithWrongEventTypesShouldNotMatch() throws Exception {
        // Given a trigger definition, which has been configured with various event types
        DeployDbTrigger trigger = new DeployDbTrigger();
        trigger.setTriggerEventTypes(Arrays.asList(
                createEvent(DeploymentCreatedEvent.class),
                createEvent(DeploymentCompletedEvent.class))
        );

        // Then it should not match against a "deployment started" webhook
        assertThat(trigger.accepts(job, hook), is(false));
    }

    @Test public void triggerWithDesiredEventTypeShouldMatch() throws Exception {
        // Given a trigger definition which has been configured with various event types, including "deployment started"
        DeployDbTrigger trigger = new DeployDbTrigger();
        trigger.setTriggerEventTypes(Arrays.asList(
                createEvent(DeploymentCreatedEvent.class),
                createEvent(DeploymentStartedEvent.class),
                createEvent(DeploymentStartedEvent.class, true),
                createEvent(PromotionCompletedEvent.class))
        );

        // Then it should match against a "deployment started" webhook
        assertThat(trigger.accepts(job, hook), is(true));
    }

    private static DeployDbTriggerEvent createEvent(Class<? extends DeployDbTriggerEvent> cls) {
        return createEvent(cls, false);
    }

    /** @return A trigger event type mock, which will either match against no criteria, or any criteria. */
    private static DeployDbTriggerEvent createEvent(Class<? extends DeployDbTriggerEvent> cls, boolean shouldMatch) {
        DeployDbTriggerEvent event = mock(cls);
        when(event.accepts(any(AbstractProject.class), any(TriggerWebhook.class))).thenReturn(shouldMatch);
        return event;
    }

}