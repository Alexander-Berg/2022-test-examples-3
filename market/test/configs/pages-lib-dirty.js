const webpackMerge = require('webpack-merge');

const browser = require('../../lib/generate/browser');
const paths = require('../lib/paths');
const {commonConfig, reactExternals, environment, splitChunksConfig} = require('../utils/configs');

module.exports = webpackMerge(browser({
    ...commonConfig,
    ...splitChunksConfig,

    roots: [paths.pages],
    browserList: ['node 12'],
    distPath: `${paths.results}/${environment}/pages-lib-dirty`,
}), {
    externals: reactExternals,
});
