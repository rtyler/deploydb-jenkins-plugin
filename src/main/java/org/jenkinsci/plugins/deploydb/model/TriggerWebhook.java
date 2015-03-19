package org.jenkinsci.plugins.deploydb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Represents a webhook sent from DeployDB in order to trigger Jenkins builds. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerWebhook {

    // TODO: Type of hook
    private long id;
    private String service;

    public long getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    @Override
    public String toString() {
        return String.format("Webhook{id=%s, service=%s}", id, service);
    }

}
