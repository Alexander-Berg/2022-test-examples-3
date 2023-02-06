'use strict';

const {
    root,
    option,
    section
} = require('gemini-configparser');

const PLUGIN_NAME = 'hermione-broken-test';

module.exports = {
    parse: root(section({
        enabled: option({
            validate(value) {
                if (['true', 'false'].indexOf(value) === -1 && typeof value !== 'boolean') {
                    throw new Error(`[${PLUGIN_NAME}]: "enabled" should be a boolean or string "true"/"false"`);
                }
            },
        }),
        mode: option({
            validate(value) {
                if (typeof value !== 'string' || ['collect', 'filter'].indexOf(value) === -1) {
                    throw new Error(`[${PLUGIN_NAME}]: "mode" should be "collect" or "filter"`);
                }
            }
        }),
        reportDir: option({
            validate(value) {
                if (typeof value !== 'string') {
                    throw new Error(`[${PLUGIN_NAME}]: "reportDir" is not specified`);
                }
            }
        }),
        reportFilename: option({
            validate(value) {
                if (typeof value !== 'string') {
                    throw new Error(`[${PLUGIN_NAME}]: "reportFileName" is not specified`);
                }
            },
            defaultValue: 'broken-tests.txt',
        }),
    }), {
        envPrefix: 'hermione-broken-tests_',
        cliPrefix: 'hermione-broken-tests-',
    })
};
