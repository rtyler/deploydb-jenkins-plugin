package org.jenkinsci.plugins.deploydb.model;

import com.google.common.annotations.VisibleForTesting;

public enum EventType {

    DEPLOYMENT_CREATED("application/vnd.deploydb.deploymentcreated.v1+json"),
    DEPLOYMENT_STARTED("application/vnd.deploydb.deploymentstarted.v1+json"),
    DEPLOYMENT_COMPLETED("application/vnd.deploydb.deploymentcompleted.v1+json"),
    PROMOTION_COMPLETED("application/vnd.deploydb.promotioncompleted.v1+json");

    private final String mimeType;

    private EventType(String mimeType) {
        this.mimeType = mimeType;
    }

    @VisibleForTesting
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the appropriate event type for a given MIME type.
     *
     * @param mimeType MIME type value.
     * @return The matching value, or {@code null} if no match could be made.
     */
    public static EventType forMimeType(String mimeType) {
        for (EventType e : values()) {
            if (e.mimeType.equalsIgnoreCase(mimeType)) {
                return e;
            }
        }
        return null;
    }

}