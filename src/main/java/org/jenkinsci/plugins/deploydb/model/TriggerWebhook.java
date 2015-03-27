package org.jenkinsci.plugins.deploydb.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/** Represents a webhook sent from DeployDB in order to trigger Jenkins builds. */
public class TriggerWebhook {

    private EventType eventType;
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

    public EventType getEventType() {
        return eventType;
    }

    /**
     * Sets the event type for this hook, based on the given MIME type.
     *
     * @param mimeType MIME type for this hook.
     * @return {@code true} iff the given value could be mapped to a known {@link EventType}.
     */
    public boolean setType(String mimeType) {
        eventType = EventType.forMimeType(mimeType);
        return eventType != null;
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
        return String.format("Webhook{type=%s, id=%s, service=%s}", eventType, id, service);
    }

}
