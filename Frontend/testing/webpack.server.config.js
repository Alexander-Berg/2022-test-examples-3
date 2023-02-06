const path = require('path');
const webpack = require('webpack');
const CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');

const getEnvironment = require('../common/env');
const env = getEnvironment({ publicUrl: '', isServer: true });

const publicPath = '/';

module.exports = [{
    mode: 'production',
    optimization: {
        minimize: false,
        concatenateModules: false, // Предотвращаем изменение имён функций и классов для успешной гидрации компонентов.
    },
    name: 'server',
    entry: {
        applications: path.resolve('platform', '.tmp', 'applications.registry.ts'),
        features: path.resolve('platform', '.tmp', 'features.registry.ts'),
        router: path.resolve('src', 'core', 'router', 'router.ts'),
        pageResources: path.resolve('src', 'core', 'pageResources', 'index.ts'),
        pages: path.resolve('src', 'core', 'pages', 'index.ts'),
        baobab: path.resolve('src', 'core', 'baobab', 'index.ts'),
        counters: path.resolve('src', 'core', 'counters', 'index.ts'),
        all: path.resolve('src', 'components', 'registry.server.ts'),
    },
    output: {
        path: path.resolve('pages/server'),
        pathinfo: true,
        filename: entry => {
            if (entry.chunk.name === 'all') {
                return 'all.react.js';
            }

            return '[name].js';
        },
        chunkFilename: '[id].chunk.js',
        publicPath: publicPath,
        libraryTarget: 'commonjs2',
    },
    resolve: {
        modules: ['node_modules'],
        extensions: ['.js', '.jsx', '.json', '.ts', '.tsx', '.scss'],
    },
    externals: {
        '@yandex-int/i18n': {
            commonjs2: '@yandex-int/i18n',
        },
    },
    module: {
        rules: [
            {
                test: /\.(ts|tsx|jsx)$/,
                use: [
                    {
                        loader: 'thread-loader',
                        options: {
                            workers: require('os').cpus().length - 1,
                            workerNodeArgs: ['--max-old-space-size=2048'],
                        },
                    },
                    {
                        loader: 'ts-loader',
                        options: {
                            happyPackMode: true,
                            configFile: path.resolve('tsconfig.json'),
                        },
                    },

                ],
                exclude: [
                    path.resolve('src', 'bundles'),
                ],
            },
            {
                test: /\.s?css$/,
                loader: 'ignore-loader',
            },
            {
                test: /\.(svg|inline\.\w+)$/,
                loader: 'raw-loader',
            },
            {
                /** При использовании пакета mjml возникает проблема со сборкой: "MJML uses
                 *  html-minifier, which uses uglify-js, and uglify-js is not compatible
                 *  with Webpack" (https://github.com/mjmlio/mjml/issues/1365).
                 *  Вариант решения это проблемы - с помощью null-loader-а
                 */
                test: /node_modules\/uglify-js\/tools\/node\.js/,
                loader: 'null-loader',
            },
        ],
    },
    plugins: [
        new webpack.DefinePlugin(env.stringified),
        new CaseSensitivePathsPlugin(),
    ],
    node: {
        fs: 'empty',
        net: 'empty',
        tls: 'empty',
        __dirname: false,
    },
    performance: {
        hints: false,
    },
    target: 'node',
}];
