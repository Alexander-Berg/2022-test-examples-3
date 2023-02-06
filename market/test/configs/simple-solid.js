const webpackMerge = require('webpack-merge');

const browser = require('../../lib/generate/browser');
const paths = require('../lib/paths');
const {commonConfig, environment, reactExternals, entrypointsConfig} = require('../utils/configs');

module.exports = webpackMerge(browser({
    ...commonConfig,
    ...entrypointsConfig,

    browserList: ['node 12'],
    distPath: `${paths.results}/${environment}/simple-solid`,
    maxInlineImageSize: 10000,
}), {
    externals: reactExternals,
});
