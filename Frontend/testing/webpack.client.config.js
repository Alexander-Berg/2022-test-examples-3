const { resolve } = require('path');
const merge = require('webpack-merge');

const CssoWebpackPlugin = require('csso-webpack-plugin').default;
const TerserPlugin = require('terser-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const OptimizeCssNanoPlugin = require('@intervolga/optimize-cssnano-plugin');
const TurboAMDPlugin = require('../common/TurboAMDPlugin');

const { getExternals } = require('../common/externals');

const commonConfig = require('../common/webpack.config');

module.exports = merge(
    commonConfig,
    {
        name: 'bundles',
        mode: 'production',
        devtool: false,

        externals: getExternals(),

        output: {
            library: '[name]',
            libraryTarget: 'amd',
            filename: '[name].js',
            chunkFilename: 'hashed_[contenthash].js',
            crossOriginLoading: 'anonymous',
        },

        module: {
            rules: [
                {
                    test: /\.s?css$/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        'css-loader',
                        {
                            loader: 'postcss-loader',
                            options: {
                                config: { path: resolve('./postcss.config') },
                            },
                        },
                    ],
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

        optimization: {
            namedChunks: true,
            moduleIds: 'hashed',
            minimizer: [
                new TerserPlugin({
                    terserOptions: { output: { comments: false } },
                    parallel: true,
                }),
            ],
        },

        plugins: [
            new TurboAMDPlugin(),
            new MiniCssExtractPlugin({
                filename: '[name].css',
                // [id] нужен, чтобы корректно работали сервисные бандлы
                chunkFilename: 'hashed_[contenthash].[id].chunk.css',
            }),
            new CssoWebpackPlugin(),
            new OptimizeCssNanoPlugin({
                cssnanoOptions: {
                    preset: ['default', {
                        discardComments: {
                            removeAll: true,
                        },
                    }],
                },
            }),
        ],
    }
);
