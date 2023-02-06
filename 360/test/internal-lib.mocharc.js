'use strict';

const fs = require('fs');
const path = require('path');

module.exports = {
    spec: [
        'internal-lib/!(node_modules)/**/*.spec.js'
    ],
    require: [
        './test/helpers/chai.js',
        './test/helpers/stub-global-config.js',
        './test/helpers/stub-yandex-geobase.js',
        './test/helpers/stub-posix.js',
        './test/helpers/stub-secrets.js',
        './test/helpers/stub-langdetect.js',
        './test/helpers/stub-express-uatraits.js'
    ],
    ui: 'bdd',
    timeout: 30000,
    reporter: 'mocha-multi-reporters',
    reporterOptions: 'configFile=test/internal-lib.mocharc.js',
    reporterEnabled: 'spec'
};

if (process.env.NEW_CI) {
    const folder = '__reports/mocha-internal-lib';
    fs.mkdirSync(path.resolve(folder), { recursive: true });

    module.exports.reporterEnabled += ', mocha-simple-html-reporter';
    module.exports.mochaSimpleHtmlReporterReporterOptions = {
        output: `${folder}/index.html`
    };
}
