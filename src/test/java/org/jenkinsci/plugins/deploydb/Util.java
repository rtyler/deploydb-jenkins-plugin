package org.jenkinsci.plugins.deploydb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jenkinsci.plugins.deploydb.model.EventType;
import org.jenkinsci.plugins.deploydb.model.TriggerWebhook;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Util {

    /** @return A webhook mock configured with the given event type. */
    public static TriggerWebhook createWebhook(EventType eventType) {
        return createWebhook(eventType, "foo");
    }

    /** @return A webhook mock configured with the given event type and service name. */
    public static TriggerWebhook createWebhook(EventType eventType, String serviceName) {
        TriggerWebhook hook = mock(TriggerWebhook.class);
        when(hook.getEventType()).thenReturn(eventType);
        when(hook.getService()).thenReturn(serviceName);
        return hook;
    }

    /** @return A request webhook object built from the contents of the given file. */
    public static TriggerWebhook getWebhook(String filename) throws IOException {
        return new ObjectMapper().readValue(Util.class.getResourceAsStream(filename), TriggerWebhook.class);
    }

    /** @return A request webhook object built from the contents of the given file, with the given type. */
    public static TriggerWebhook getWebhook(String filename, EventType type) throws IOException {
        TriggerWebhook hook =
                new ObjectMapper().readValue(Util.class.getResourceAsStream(filename), TriggerWebhook.class);
        hook.setType(type.getMimeType());
        return hook;
    }

}
