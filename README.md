# DeployDB Plugin for Jenkins

[![Build Status](https://travis-ci.org/lookout/deploydb-jenkins-plugin.svg?branch=master)](https://travis-ci.org/lookout/deploydb-jenkins-plugin)

Triggers builds based on incoming [DeployDB][] webhooks, and reports the results back to DeployDB.

## Webhook configuration
Your DeployDB installation should be configured to send all webhook events to
    `$JENKINS_BASE_URL/deploydb/trigger`

Jenkins should be configured with the base URL to your DeployDB instance,
so that it knows where build results should be reported to: Manage Jenkins → Configure System → DeployDB.

## Development
This plugin is built with the [Jenkins Gradle plugin][jpi-plugin].

To start an instance of Jenkins at http://localhost:8080/ with the plugin installed:  
`./gradlew server`

To run the test cases:  
`./gradlew test`

To build the plugin, ready for installation into Jenkins:  
`./gradlew jpi`

The plugin will be written to `build/libs/deploydb-jenkins.hpi`.

## Release
### One-time preparation
* You must have a [Jenkins account][jenkins-account]
* Add your credentials to `~/.jenkins-ci.org` in the following form:
```
userName=...
password=...
```

### For each release
To publish a plugin update to the Jenkins update centre:
* Update the top-level `version` property in `build.gradle`
* Commit: `git commit -am "Preparing for release ${VERSION}."`
* Tag: `git tag deploydb-jenkins-plugin-${VERSION}`
* Push: `git push && git push --tags`
* Release: `./gradlew clean publish`

Assuming the Gradle build went well, you can confirm here that the release was uploaded correctly:
http://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/deploydb/

The plugin update should appear in the Jenkins update centre within four to eight hours.

[deploydb]:https://github.com/lookout/deploydb
[jpi-plugin]:https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin
[jenkins-account]:https://jenkins-ci.org/account/