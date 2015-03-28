package org.jenkinsci.plugins.deploydb.model.events;

import hudson.Extension;
import org.jenkinsci.plugins.deploydb.Messages;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.kohsuke.stapler.DataBoundConstructor;

public class DeploymentCompletedEvent extends DeployDbTriggerEvent {

    @DataBoundConstructor
    public DeploymentCompletedEvent() {}

    @Override
    public EventType getEventType() {
        return EventType.DEPLOYMENT_COMPLETED;
    }

    @Override protected Class<? extends EventDescriptor> getDescriptorClass() {
        return DeploymentCompletedEventDescriptor.class;
    }

    @Extension
    public static class DeploymentCompletedEventDescriptor extends EventDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TriggerEventDeploymentCompleted();
        }

    }

}
