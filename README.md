# DeployDB Plugin for Jenkins

Triggers builds based on incoming [DeployDB][] webhooks, and reports the results back to DeployDB.

## Webhook configuration
Your DeployDB installation should be configured to send all webhook events to
    `$JENKINS_BASE_URL/deploydb/trigger`

## Development
This plugin is built with the [Jenkins Gradle plugin][jpi-plugin].

To start an instance of Jenkins at http://localhost:8080/ with the plugin installed:  
`./gradlew server`

To run the test cases:  
`./gradlew test`

To build the plugin, ready for installation into Jenkins:  
`./gradlew jpi`

The plugin will be written to `build/libs/deploydb-jenkins.hpi`.

[deploydb]:https://github.com/lookout/deploydb
[jpi-plugin]:https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin
