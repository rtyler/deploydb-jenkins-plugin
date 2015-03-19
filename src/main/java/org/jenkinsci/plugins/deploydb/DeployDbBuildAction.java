package org.jenkinsci.plugins.deploydb;

import com.google.common.base.CaseFormat;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;

import java.util.Map;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static hudson.Util.fixNull;

/** Contains the data required for a DeployDB-triggered build, and exports it to the build environment. */
public class DeployDbBuildAction implements EnvironmentContributingAction {

    /** Prefix to apply to all environment variables this action exports. */
    static final String ENV_VAR_PREFIX = "DDB_";

    private final TriggerWebhook hook;

    public DeployDbBuildAction(TriggerWebhook hook) {
        this.hook = hook;
    }

    /** @return The webhook that triggered the build to which this action is attached. */
    public TriggerWebhook getHook() {
        return hook;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        // Export the common environment variables
        env.put(getEnvKey("eventId"), String.valueOf(hook.getId()));
        env.put(getEnvKey("service"), hook.getService());

        // Recursively export all other key/value pairs in the hook payload
        exportHookValues(env, hook.getOtherValues());
    }

    private static void exportHookValues(EnvVars env, Map<String, Object> values) {
        exportHookValues(env, null, values);
    }

    /**
     * Recursively adds all key/values from the given map to the environment.
     *
     * @param env Environment to add to.
     * @param nestingPrefix Optional prefix to add to each key, if the values map contains a nested map.
     * @param values Key/value pairs to be added to the environment.
     */
    private static void exportHookValues(EnvVars env, String nestingPrefix, Map<String, Object> values) {
        if (env == null || values == null) {
            return;
        }

        nestingPrefix = fixNull(nestingPrefix).trim();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                exportHookValues(env, nestingPrefix + entry.getKey() + "_", (Map<String, Object>) value);
            } else {
                env.put(getEnvKey(entry.getKey(), nestingPrefix), String.valueOf(value));
            }
        }
    }

    private static String getEnvKey(String key) {
        return getEnvKey(key, null);
    }

    /**
     * Turns a key and optional prefix into a DeployDB environment variable name.
     * <p/>
     * Values in {@code camelCase} will be converted to be {@code UNDERSCORE_SEPARATED}.
     *
     * @param key Value, possibly camel-cased.
     * @param nestingPrefix Optional string prefix, possibly camel-cased, e.g. {@code artifact} or {@code artifactInfo}.
     * @return A value prefixed with {@link #ENV_VAR_PREFIX}, e.g. given the parameters {@code key=buildId} and
     *         {@code nestingPrefix=artifact}, {@code DDB_ARTIFACT_BUILD_ID} would be returned.
     */
    private static String getEnvKey(String key, String nestingPrefix) {
        nestingPrefix = fixNull(nestingPrefix).trim();
        return ENV_VAR_PREFIX + LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, nestingPrefix + key);
    }

    // Not needed; this is not a UI-facing Action

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

}
