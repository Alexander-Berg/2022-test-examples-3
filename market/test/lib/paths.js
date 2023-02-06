const Path = require('path');

const filesRoot = Path.resolve(__dirname, '..', 'files');

module.exports = {
    webpackBin: Path.resolve(__dirname, '..', '..', 'node_modules', '.bin', 'webpack'),
    filesRoot,
    app: Path.resolve(filesRoot, 'app'),
    commons: Path.resolve(filesRoot, 'commons'),
    lib: Path.resolve(filesRoot, 'lib'),
    pages: Path.resolve(filesRoot, 'pages'),
    vendors: Path.resolve(filesRoot, 'vendors'),
    resolvers: Path.resolve(filesRoot, 'resolvers'),
    nodeModules: Path.resolve(filesRoot, 'node_modules'),

    configs: Path.resolve(__dirname, '..', 'configs'),
    results: Path.resolve(__dirname, '..', 'results'),
    snapshots: Path.resolve(__dirname, '..', 'snapshots'),

    babelBrowser: require.resolve('../../lib/babel/browser'),
    babelNode: require.resolve('../../lib/babel/nodejs'),
    babelJest: require.resolve('../../lib/babel/jest'),
    postCss: require.resolve('../../lib/postcss/postcss.config.js'),
};
