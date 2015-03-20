package org.jenkinsci.plugins.deploydb;

import hudson.model.Cause;

public class DeployDbCause extends Cause {

    @Override
    public String getShortDescription() {
        // Text shown in the badge on the build page
        return Messages.Cause();
    }

}
