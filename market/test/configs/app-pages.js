const webpackMerge = require('webpack-merge');

const server = require('../../lib/generate/server');
const paths = require('../lib/paths');
const {commonConfig, nodeExternals, environment} = require('../utils/configs');

module.exports = webpackMerge(server(({
    ...commonConfig,

    appEntry: `${paths.app}/pages.js`,
    distPath: `${paths.results}/${environment}/app-pages`,
})), {
    externals: ['react', nodeExternals],
});
