package org.jenkinsci.plugins.deploydb.model.events;

import hudson.Extension;
import org.jenkinsci.plugins.deploydb.Messages;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.kohsuke.stapler.DataBoundConstructor;

public class DeploymentCreatedEvent extends DeployDbTriggerEvent {

    @DataBoundConstructor
    public DeploymentCreatedEvent() {}

    @Override
    public EventType getEventType() {
        return EventType.DEPLOYMENT_CREATED;
    }

    @Override protected Class<? extends EventDescriptor> getDescriptorClass() {
        return DeploymentCreatedEventDescriptor.class;
    }

    @Extension
    public static class DeploymentCreatedEventDescriptor extends EventDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TriggerEventDeploymentCreated();
        }

    }

}
