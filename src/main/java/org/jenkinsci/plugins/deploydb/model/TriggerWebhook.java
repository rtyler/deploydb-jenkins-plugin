package org.jenkinsci.plugins.deploydb.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/** Represents a webhook sent from DeployDB in order to trigger Jenkins builds. */
public class TriggerWebhook {

    // TODO: Type of hook
    private long id;
    private String service;
    private Map<String, Object> map;

    private TriggerWebhook() {
        this.map = new HashMap<String, Object>();
    }

    public long getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    /** Allows Jackson to set hook key/values we're not explicitly interested in. */
    @JsonAnySetter
    public void setOtherValue(String name, Object value) {
        map.put(name, value);
    }

    /** @return A map of the payload fields we're not explicitly interested in. */
    public Map<String, Object> getOtherValues() {
        return map;
    }

    @Override
    public String toString() {
        return String.format("Webhook{id=%s, service=%s, values=%s}", id, service, map);
    }

}
