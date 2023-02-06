const getNodeExternals = require('webpack-node-externals');

const paths = require('../lib/paths');

const environment = process.env.BUILD_ENV;

const commonConfig = {
    environment,
    repoRoot: paths.filesRoot,
    resolversPath: paths.resolvers,
    postcssConfig: paths.postCss,
    babelConfig: paths.babelBrowser,
};

const splitChunksConfig = {
    splitChunks: {
        cacheGroups: {
            vendors: {
                chunks: 'all',
                enforce: true,
                test: /\/(vendors|node_modules)\//,
            },
        },
    },
};

const entrypointsConfig = {
    entrypoints: {
        first: `${paths.filesRoot}/first`,
        second: `${paths.filesRoot}/second`,
        third: `${paths.filesRoot}/third`,
    },
};

const reactExternals = {
    react: 'React',
    'react-dom': 'ReactDOM',
};

const nodeExternals = getNodeExternals({
    modulesDir: paths.nodeModules,
});

module.exports = {
    environment,
    commonConfig,
    splitChunksConfig,
    entrypointsConfig,
    reactExternals,
    nodeExternals,
};
