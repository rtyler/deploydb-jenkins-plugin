package org.jenkinsci.plugins.deploydb;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import jenkins.model.GlobalConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class DeployDbConfigTest {

    @Rule public final JenkinsRule jenkins = new JenkinsRule();

    @Test public void submittingGlobalConfigSetsUrl() throws Exception {
        final String baseUrl = "https://ddb.example.com:8443/deploydb";

        // Given that no base URL has been configured
        DeployDbConfig ddbConfig = GlobalConfiguration.all().get(DeployDbConfig.class);
        assertThat(ddbConfig.getBaseUrl(), is(nullValue()));

        // When the Jenkins global config page is submitted with a URL
        HtmlForm form = jenkins.createWebClient().goTo("configure").getFormByName("config");
        HtmlInput field = form.getInputByName("_.baseUrl");
        field.setValueAttribute(baseUrl);
        jenkins.submit(form);

        // Then the correct URL should have been set and persisted
        assertThat(ddbConfig.getBaseUrl(), is(baseUrl));
    }

    @Test public void loadingGlobalConfigShowsPersistedUrl() throws Exception {
        final String baseUrl = "http://example.com/";

        // Given that a base URL has been configured
        DeployDbConfig ddbConfig = GlobalConfiguration.all().get(DeployDbConfig.class);
        ddbConfig.setBaseUrl(baseUrl);

        // When the Jenkins global config page is loaded
        HtmlForm form = jenkins.createWebClient().goTo("configure").getFormByName("config");

        // Then the config field should display the correct URL
        HtmlInput field = form.getInputByName("_.baseUrl");
        assertThat(field.getValueAttribute(), is(baseUrl));
    }

}