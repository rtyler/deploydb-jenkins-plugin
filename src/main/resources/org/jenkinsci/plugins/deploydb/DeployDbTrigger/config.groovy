package org.jenkinsci.plugins.deploydb.DeployDbTrigger;

f = namespace('/lib/form')

f.entry(title: _("Event types")) {
    f.hetero_list(descriptors: descriptor.eventDescriptors, items: instance.triggerEventTypes,
                  name: 'triggerEventTypes', hasHeader: true)
}