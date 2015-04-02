package org.jenkinsci.plugins.deploydb;

import hudson.EnvVars;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.deploydb.Util.getWebhook;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DeployDbBuildActionTest {

    @Test public void jsonPropertiesShouldBeExportedToEnvironment() throws Exception {
        // Given a build action has been created from a JSON webhook payload
        TriggerWebhook hook = getWebhook("hook_trigger_deployment_started.json");
        DeployDbBuildAction action = new DeployDbBuildAction(hook);

        // When we generate the environment variables
        final EnvVars env = new EnvVars();
        action.buildEnvVars(null, env);

        // Then the environment should contain the only the hook-provided values
        assertThat(env.size(), is(11));

        // And the basic properties we care about should exist in the environment
        assertThat(env.get("DDB_EVENT_ID"), is("1"));
        assertThat(env.get("DDB_SERVICE"), is("faas"));

        // And other top-level items should have been added
        assertThat(env.get("DDB_STATUS"), is("STARTED"));
        assertThat(env.get("DDB_ENVIRONMENT"), is("pre-prod"));
        assertThat(env.get("DDB_CREATED_AT"), is("2015-03-14T09:26:53+00:00"));

        // And nested objects from the JSON should also have been added, each with the object's key as prefix
        assertThat(env.get("DDB_ARTIFACT_ID"), is("2"));
        assertThat(env.get("DDB_ARTIFACT_GROUP"), is("com.example.cucumber"));
        assertThat(env.get("DDB_ARTIFACT_NAME"), is("cucumber-artifact"));
        assertThat(env.get("DDB_ARTIFACT_VERSION"), is("1.0.1"));
        assertEquals(
                "http://example.com/maven/com.example.cucumber/cucumber-artifact/1.0.1/cucumber-artifact-1.0.1.jar",
                env.get("DDB_ARTIFACT_SOURCE_URL"));
        assertThat(env.get("DDB_ARTIFACT_CREATED_AT"), is("2015-03-14T09:26:53+00:00"));
    }

    @Test public void arbitraryNestedValuesShouldBeExportedToEnvironment() throws Exception {
        // Given a build action has been created from a JSON webhook payload
        TriggerWebhook hook = getWebhook("hook_trigger_nested.json");
        DeployDbBuildAction action = new DeployDbBuildAction(hook);

        // When we generate the environment variables
        final EnvVars env = new EnvVars();
        action.buildEnvVars(null, env);

        // Then the environment should contain the only the hook-provided values
        assertThat(env.size(), is(6));

        // And arbitrary levels of nesting should work
        assertThat(env.get("DDB_EVENT_ID"), is("1"));
        assertThat(env.get("DDB_SERVICE"), is("faas"));
        assertThat(env.get("DDB_FOO_BAR_ID"), is("2"));

        // And maps whose keys are camel-cased should appear as underscore-separated values
        assertThat(env.get("DDB_FOO_BAR_CHILD_NESTED_ITEM_PARENT_ID"), is("2"));
        assertThat(env.get("DDB_FOO_BAR_CHILD_NESTED_ITEM_NESTED"), is("very"));
        assertThat(env.get("DDB_FOO_BAR_CHILD_NESTED_ITEM_LEAF"), is("true"));
    }

}