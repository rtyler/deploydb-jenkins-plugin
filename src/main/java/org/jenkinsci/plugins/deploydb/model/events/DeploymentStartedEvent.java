package org.jenkinsci.plugins.deploydb.model.events;

import hudson.Extension;
import org.jenkinsci.plugins.deploydb.Messages;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.kohsuke.stapler.DataBoundConstructor;

public class DeploymentStartedEvent extends DeployDbTriggerEvent {

    @DataBoundConstructor
    public DeploymentStartedEvent() {}

    @Override
    public EventType getEventType() {
        return EventType.DEPLOYMENT_STARTED;
    }

    @Override protected Class<? extends EventDescriptor> getDescriptorClass() {
        return DeploymentStartedEventDescriptor.class;
    }

    @Extension
    public static class DeploymentStartedEventDescriptor extends EventDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TriggerEventDeploymentStarted();
        }

    }

}
