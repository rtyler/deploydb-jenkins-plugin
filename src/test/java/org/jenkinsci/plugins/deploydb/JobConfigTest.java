package org.jenkinsci.plugins.deploydb;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.AbstractProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;

public class JobConfigTest {

    @Rule public final JenkinsRule jenkins = new JenkinsRule();

    @Test public void newJobConfigurationShouldNotFail() throws Exception {
        // Given we create a brand new job
        AbstractProject<?, ?> job = jenkins.createFreeStyleProject();

        // When we open its configuration page
        HtmlPage page = jenkins.createWebClient().getPage(job, "configure");

        // Then nothing untoward should happen (i.e. no 500 error)
        assertNotNull(page);
    }

}