const webpackMerge = require('webpack-merge');

const browser = require('../../lib/generate/browser');
const paths = require('../lib/paths');
const {commonConfig, environment, reactExternals, splitChunksConfig, entrypointsConfig} = require('../utils/configs');

module.exports = webpackMerge(browser({
    ...commonConfig,
    ...splitChunksConfig,
    ...entrypointsConfig,

    browserList: ['node 12'],
    distPath: `${paths.results}/${environment}/simple-lib-clean`,
}), {
    externals: reactExternals,
});
