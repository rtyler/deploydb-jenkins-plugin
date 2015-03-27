package org.jenkinsci.plugins.deploydb.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.deploydb.model.EventType.forMimeType;
import static org.junit.Assert.assertThat;

public class EventTypeTest {

    @Test public void emptyMimeTypesReturnNull() {
        assertThat(forMimeType(null), is(nullValue()));
        assertThat(forMimeType(""), is(nullValue()));
    }

    @Test public void unknownMimeTypesReturnNull() {
        assertThat(forMimeType("foo"), is(nullValue()));
        assertThat(forMimeType("application/json"), is(nullValue()));
        assertThat(forMimeType("application/vnd.deploydb.deploymentcreated.v0+xml"), is(nullValue()));
    }

    @Test public void validMimeTypesMatch() {
        for (EventType event : EventType.values()) {
            assertThat(forMimeType(event.getMimeType()), is(event));
        }
    }

}