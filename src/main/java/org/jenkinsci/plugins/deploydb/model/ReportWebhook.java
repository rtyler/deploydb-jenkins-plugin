package org.jenkinsci.plugins.deploydb.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/** Represents a webhook sent to DeployDB from Jenkins to report build status. */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReportWebhook {

    private final String name;
    private final Status status;
    private final String infoUrl;

    public ReportWebhook(String jobName, String buildUrl, boolean wasSuccessful) {
        this.name = jobName;
        this.infoUrl = buildUrl;
        this.status = wasSuccessful ? Status.SUCCESS : Status.FAILURE;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public String getInfoUrl() {
        return infoUrl;
    }

    @Override
    public String toString() {
        return String.format("ReportWebhook{name=%s, status=%s, infoUrl=%s}", name, status, infoUrl);
    }

    public static enum Status {
        SUCCESS,
        FAILURE
    }

}
