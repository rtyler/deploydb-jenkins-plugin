package org.jenkinsci.plugins.deploydb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lookout.whoas.AbstractHookQueue;
import com.github.lookout.whoas.HookRequest;
import com.github.lookout.whoas.WhoasFactory;
import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.deploydb.model.ReportWebhook;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.logging.Logger;

import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.removeTrailingSlash;
import static java.util.Locale.ROOT;

/** Listens for the completion of DeployDB-triggered builds and reports the result back to DeployDB. */
@Extension
public class BuildCompletionListener extends RunListener<AbstractBuild<?, ?>> {

    static final Logger LOGGER = Logger.getLogger(BuildCompletionListener.class.getName());

    /** Appended to the DeployDB base URL, this forms the URL to send report webhooks to. */
    private static final String REPORT_PATH_TEMPLATE = "/api/deployments/%d/promotions";

    /** Period in milliseconds to wait before re-attempting to enqueue a webhook, if doing so failed. */
    private static final long DELIVERY_RETRY_INTERVAL = 5 * 1000;

    /** Number of times to attempt to enqueue a webhook for delivery. */
    static final int MAX_DELIVERY_ATTEMPTS = 3;

    @Inject private DeployDbConfig config;

    @Inject private WhoasFactory whoasFactory;

    private AbstractHookQueue webhookQueue;

    @VisibleForTesting
    void setWhoasFactory(WhoasFactory factory) {
        this.whoasFactory = factory;
    }

    private AbstractHookQueue getWebhookQueue() {
        // Check whether the queue has already been set up
        if (webhookQueue != null) {
            return webhookQueue;
        }

        // Set up and start processing the webhook publisher queue
        webhookQueue = whoasFactory.buildQueue();
        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                whoasFactory.buildRunner(webhookQueue).run();
            }
        });
        thread.setName("DeployDB webhook publisher");
        thread.start();

        // All done!
        return webhookQueue;
    }

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, @Nonnull TaskListener listener) {
        // Ignore any builds that were not triggered by DeployDB
        final DeployDbBuildAction action = build.getAction(DeployDbBuildAction.class);
        if (action == null) {
            return;
        }

        // Check whether we know where to find DeployDB
        final String baseUrl = fixEmptyAndTrim(config.getBaseUrl());
        if (baseUrl == null || !baseUrl.startsWith("http")) {
            LOGGER.warning("Cannot report build result to DeployDB as no base URL has been configured.");
            return;
        }

        // TODO: Ignore builds configured with silent mode

        // Create and send a report webhook to DeployDB for this build
        long deploymentId = action.getHook().getId();
        String reportUrl = removeTrailingSlash(baseUrl) + String.format(ROOT, REPORT_PATH_TEMPLATE, deploymentId);
        final ReportWebhook hook = buildReportWebhook(build);
        try {
            sendReportWebhook(reportUrl, hook);
        } catch (Exception e) {
            // Should never happen as the JSON is very simple, but ensure the failure is logged
            LOGGER.severe(String.format("Failed to serialise report %s to JSON: %s", hook, e));
        }
    }

    /**
     * Sends the given report webhook to the webhook publisher.
     *
     * @param reportUrl The DeployDB URL to which the webhook should be POSTed.
     * @param hook The report to be sent.
     */
    private void sendReportWebhook(String reportUrl, ReportWebhook hook) throws JsonProcessingException,
            InterruptedException {
        // Serialise the JSON and build the hook request to be delivered
        final String json = new ObjectMapper().writeValueAsString(hook);
        final HookRequest request = new HookRequest(reportUrl, json, "application/json");
        final AbstractHookQueue webhookQueue = getWebhookQueue();

        int attempts = 0;
        do {
            // Attempt to enqueue the payload for delivery
            boolean hookQueued = webhookQueue.push(request);

            // If the queue accepted the hook, we're done
            if (hookQueued) {
                LOGGER.fine(String.format("Successfully enqueued %s for delivery to %s", hook, reportUrl));
                return;
            }

            // Otherwise sleep for a moment
            Thread.sleep(DELIVERY_RETRY_INTERVAL);
        } while (++attempts < MAX_DELIVERY_ATTEMPTS);

        // If we ended up here, the webhook could not be enqueued
        LOGGER.warning(String.format("Failed to enqueue %s for delivery to %s", hook, reportUrl));
    }

    /** @return A report webhook with the appropriate data for the given build. */
    static ReportWebhook buildReportWebhook(AbstractBuild<?, ?> build) {
        // Any build result other than SUCCESS is considered a failure
        boolean wasSuccessful = build.getResult() == Result.SUCCESS;
        String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
        return new ReportWebhook(build.getParent().getName(), buildUrl, wasSuccessful);
    }

}
