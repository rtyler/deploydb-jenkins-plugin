package org.jenkinsci.plugins.deploydb.model.events;

import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.deploydb.Messages;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixNull;

/** Represents an event type that can be added to a DeployDbTrigger. */
public abstract class DeployDbTriggerEvent extends AbstractDescribableImpl<DeployDbTriggerEvent> {

    private static final Logger LOGGER = Logger.getLogger(DeployDbTriggerEvent.class.getName());

    private String serviceNameRegex;

    public String getServiceNameRegex() {
        return serviceNameRegex;
    }

    @DataBoundSetter
    public void setServiceNameRegex(String serviceNameRegex) {
        this.serviceNameRegex = serviceNameRegex;
    }

    /** @return {@code true} if the given webhook matches the criteria configured for this instance. */
    public boolean accepts(AbstractProject<?, ?> job, TriggerWebhook hook) {
        // If no regex has been configured, the hook can't match
        if (fixEmpty(serviceNameRegex) == null) {
            LOGGER.info(String.format("Job '%s' %s trigger will never match, as no regex has been configured.",
                    job.getDisplayName(), getClass().getSimpleName()));
            return false;
        }

        // If the regex is invalid, the hook can't match
        try {
            Pattern.compile(serviceNameRegex);
        } catch (PatternSyntaxException e) {
            LOGGER.warning(
                    String.format("Job '%s' %s trigger has been configured with an invalid regular expression '%s'.",
                            job.getDisplayName(), getClass().getSimpleName(), serviceNameRegex));
            return false;
        }

        // The type of the hook has to match the type for this class
        if (hook.getEventType() != getEventType()) {
            return false;
        }

        // If the hook provides no service name, we can't match
        final String service = fixEmptyAndTrim(hook.getService());
        if (service == null) {
            return false;
        }

        // Check whether the hook's service name matches exactly, or matches the configured regular expression
        return service.equalsIgnoreCase(serviceNameRegex) || service.matches(serviceNameRegex);
    }

    /** @return The enum value corresponding to the event type the subclass represents. */
    public abstract EventType getEventType();

    protected abstract Class<? extends EventDescriptor> getDescriptorClass();

    @Override
    public Descriptor<DeployDbTriggerEvent> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(getDescriptorClass());
    }

    public abstract static class EventDescriptor extends Descriptor<DeployDbTriggerEvent> {

        /** Displays an error in the web UI at configuration time, if the given regex does not compile. */
        public FormValidation doCheckServiceNameRegex(@QueryParameter String value) {
            String regex = fixNull(value).trim();
            try {
                Pattern.compile(regex);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.errorWithMarkup(
                        String.format("%s: <pre>%s</pre>", Messages.TriggerInvalidRegex(), e.getMessage()));
            }
        }

    }

}

