const merge = require('webpack-merge');

const CssoWebpackPlugin = require('csso-webpack-plugin').default;
const TerserPlugin = require('terser-webpack-plugin');
const OptimizeCssNanoPlugin = require('@intervolga/optimize-cssnano-plugin');
const webpackPluginHashOutput = require('webpack-plugin-hash-output');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TurboAMDPlugin = require('../common/TurboAMDPlugin');

const commonConfig = require('../common/webpack.externals.config');

module.exports = merge(
    commonConfig,
    {
        mode: 'production',
        devtool: false,

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
            new CssoWebpackPlugin(),
            new TurboAMDPlugin(),
            new OptimizeCssNanoPlugin({
                cssnanoOptions: {
                    preset: ['default', {
                        discardComments: {
                            removeAll: true,
                        },
                    }],
                },
            }),
            new MiniCssExtractPlugin({
                filename: '[name].css',
                chunkFilename: 'hashed_[contenthash].chunk.css',
            }),
            new webpackPluginHashOutput({
                validateOutput: true,
                validateOutputRegex: /hashed[\s\S]+\.js$/,
            }),
        ],
    }
);
