'use strict';

const levels = require('../levels');

module.exports = function(config) {
    /* tests */
    config.includeConfig('enb-bem-tmpl-specs');

    config.module('enb-bem-tmpl-specs')
        .createConfigurator('test:templates', {
            timeout: 5000,
            coverage: {
                engines: ['bemhtml'],
                reportDirectory: '__reports/unit/coverage/template',
                exclude: ['**/node_modules/**', '**/modules/**'],
                reporters: ['lcov']
            }
        })
        .configure({
            prependFiles: [
                require.resolve('../tests/prepend')
            ],
            appendFiles: [
                require.resolve('../tests/append')
            ],
            saveHtml: true,
            langs: true,
            destPath: '__reports/unit/templates',
            levels: [
                'blocks/desktop',
                'blocks/external'
            ],
            sourceLevels: levels(config),
            engines: {
                'bemhtml': {
                    tech: 'enb-bemxjst/techs/bemhtml',
                }
            }
        });
}
