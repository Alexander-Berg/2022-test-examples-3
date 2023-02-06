const browser = require('../../lib/generate/browser');
const paths = require('../lib/paths');
const {commonConfig, environment} = require('../utils/configs');

module.exports = browser({
    ...commonConfig,

    roots: [paths.pages],
    browserList: ['node 12'],
    distPath: `${paths.results}/${environment}/pages-solid`,
});
