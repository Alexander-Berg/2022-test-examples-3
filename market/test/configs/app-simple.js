const webpackMerge = require('webpack-merge');

const server = require('../../lib/generate/server');
const paths = require('../lib/paths');
const {commonConfig, nodeExternals, environment} = require('../utils/configs');

module.exports = webpackMerge(server(({
    ...commonConfig,

    babelConfig: paths.babelNode,
    appEntry: `${paths.app}/simple.js`,
    distPath: `${paths.results}/${environment}/app-simple`,
})), {
    externals: ['react', nodeExternals],
});
