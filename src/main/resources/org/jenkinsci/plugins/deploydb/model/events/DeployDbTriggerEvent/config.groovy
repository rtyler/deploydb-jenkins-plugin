package org.jenkinsci.plugins.deploydb.model.events.DeployDbTriggerEvent;

f = namespace(lib.FormTagLib)

f.entry(title: _("Service"), field: 'serviceNameRegex',
        description: _('Enter a service name, or a regular expression')) {
    f.textbox()
}
