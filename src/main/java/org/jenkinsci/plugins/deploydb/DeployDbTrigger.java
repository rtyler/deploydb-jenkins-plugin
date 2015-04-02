package org.jenkinsci.plugins.deploydb;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.jenkinsci.plugins.deploydb.model.events.DeployDbTriggerEvent;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;
import java.util.logging.Logger;

/** Build trigger specifying criteria to match against incoming DeployDB webhooks. */
public class DeployDbTrigger extends Trigger<AbstractProject<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(DeployDbTrigger.class.getName());

    private boolean silentMode;
    private List<DeployDbTriggerEvent> triggerEventTypes;

    @DataBoundConstructor
    public DeployDbTrigger() {}

    public boolean isSilentMode() {
        return silentMode;
    }

    @DataBoundSetter
    public void setSilentMode(boolean silent) {
        this.silentMode = silent;
    }

    public List<DeployDbTriggerEvent> getTriggerEventTypes() {
        return triggerEventTypes;
    }

    @DataBoundSetter
    public void setTriggerEventTypes(List<DeployDbTriggerEvent> triggerEventTypes) {
        this.triggerEventTypes = triggerEventTypes;
    }

    /** @return {@code true} if the given webhook matches the criteria configured for this instance. */
    public boolean accepts(AbstractProject<?, ?> job, TriggerWebhook hook) {
        // Check whether we've been configured correctly
        if (triggerEventTypes == null || triggerEventTypes.isEmpty()) {
            LOGGER.info(String.format("Job '%s' cannot match any hooks, as no event triggers have been configured.",
                    job.getDisplayName()));
            return false;
        }

        // Check whether *any* of the configured event types match
        for (DeployDbTriggerEvent e : triggerEventTypes) {
            if (e.accepts(job, hook)) {
                return true;
            }
        }

        // Nothing matched
        return false;
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        public List<DeployDbTriggerEvent.EventDescriptor> getEventDescriptors() {
            return Jenkins.getInstance().getExtensionList(DeployDbTriggerEvent.EventDescriptor.class);
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return Messages.TriggerDisplayName();
        }

    }

}
