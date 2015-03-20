package org.jenkinsci.plugins.deploydb.DeployDbConfig;

f = namespace(lib.FormTagLib)

f.section(title:_("DeployDB")) {

    f.entry(field: 'baseUrl', title:_("Base URL"), description:_("Enter the base URL of a DeployDB installation")) {
        f.textbox()
    }

}
