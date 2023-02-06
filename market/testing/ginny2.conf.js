const path = require('path');
const babelrc = require('../../../../babelrc.node');

require('@babel/register')({
    presets: babelrc.presets,
    ignore: [
        filepath =>
            filepath.includes('node_modules') &&
            !filepath.includes('app/node_modules') &&
            !filepath.includes('beton/src') &&
            !filepath.includes('seo/esm'),
    ],
});

require('ignore-styles').default(['.css', '.styl']);

module.exports = {
    hermione: path.resolve(__dirname, 'hermione2.conf.js'),

    allure: {
        targetDir: process.env.ALLURE_RESULTS_DIR || 'spec/hermione2/allure/results',
        reportDir: process.env.REPORTS_DIR || 'spec/hermione2/allure/report',
    },

    commands: {
        targetDir: 'spec/hermione/commands',
    },

    system: {
        debug: true,
    },
};
