package org.jenkinsci.plugins.deploydb;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixNull;

/** Build trigger specifying criteria to match against incoming DeployDB webhooks. */
public class DeployDbTrigger extends Trigger<AbstractProject<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(DeployDbTrigger.class.getName());

    // TODO: Event type(s)

    private String serviceNameRegex;

    @DataBoundConstructor
    public DeployDbTrigger() {}

    public String getServiceNameRegex() {
        return serviceNameRegex;
    }

    @DataBoundSetter
    public void setServiceNameRegex(String regex) {
        this.serviceNameRegex = regex;
    }

    /** @return {@code true} if the given webhook should trigger the related excuse. */
    public boolean accepts(TriggerWebhook hook) {
        LOGGER.fine(String.format("Checking whether %s matches regex '%s'...", hook, serviceNameRegex));
        String service = fixEmptyAndTrim(hook.getService());
        return service != null && (service.equals(serviceNameRegex) || service.matches(serviceNameRegex));
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        public FormValidation doCheckServiceNameRegex(@QueryParameter String value) {
            String regex = fixNull(value).trim();
            try {
                Pattern.compile(regex);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.errorWithMarkup(
                        String.format("%s: <tt>%s</tt>", Messages.TriggerInvalidRegex(), e.getMessage()));
            }
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
