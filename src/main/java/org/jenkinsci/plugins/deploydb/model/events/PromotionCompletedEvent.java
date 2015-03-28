package org.jenkinsci.plugins.deploydb.model.events;

import hudson.Extension;
import org.jenkinsci.plugins.deploydb.Messages;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.kohsuke.stapler.DataBoundConstructor;

public class PromotionCompletedEvent extends DeployDbTriggerEvent {

    @DataBoundConstructor
    public PromotionCompletedEvent() {}

    @Override
    public EventType getEventType() {
        return EventType.PROMOTION_COMPLETED;
    }

    @Override protected Class<? extends EventDescriptor> getDescriptorClass() {
        return PromotionCompletedEventDescriptor.class;
    }

    @Extension
    public static class PromotionCompletedEventDescriptor extends EventDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TriggerEventPromotionCompleted();
        }

    }

}
