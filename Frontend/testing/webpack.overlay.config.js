const { resolve } = require('path');
const merge = require('webpack-merge');

const CssoWebpackPlugin = require('csso-webpack-plugin').default;
const TerserPlugin = require('terser-webpack-plugin');
const OptimizeCssNanoPlugin = require('@intervolga/optimize-cssnano-plugin');
const webpackPluginHashOutput = require('webpack-plugin-hash-output');

const commonConfig = require('../common/webpack.overlay.config');

module.exports = merge(
    commonConfig, {
        mode: 'production',
        devtool: false,

        output: {
            chunkFilename: 'hashed_[chunkhash].js',
        },

        module: {
            rules: [{
                test: /\.s?css$/,
                use: [
                    'raw-loader',
                    {
                        loader: 'postcss-loader',
                        options: {
                            config: {
                                path: resolve('./postcss.config'),
                            },
                        },
                    },
                ],
            }],
        },

        optimization: {
            namedChunks: true,
            moduleIds: 'hashed',
            minimizer: [
                new TerserPlugin({
                    terserOptions: {
                        output: {
                            comments: false,
                        },
                        mangle: {
                            properties: {
                                reserved: [
                                    'Ya',
                                    // api поискового приложения
                                    'verticalServices',
                                    'AppsApi',
                                    'whenAvailable',
                                    'whenUnavailable',
                                    'top',
                                    'yandex',
                                    'publicFeature',
                                    'addOverrideHostEntries',

                                    // публичный api оверлея
                                    'open',
                                    'close',
                                    'prerender',
                                    'isOpen',
                                    'showCloseButton',
                                    'hideCloseButton',
                                    'showNextPage',
                                    'showPrevPage',

                                    // параметры для открытия
                                    'displayUrl',
                                    'frameUrl',
                                    'originalUrl',
                                    'hostForMetrika',
                                    'turboOverlay',

                                    // базовые данные хуков
                                    'root',
                                    'urls',
                                    'index',
                                    'overlayIndex',
                                    'meta',

                                    // данные для fallback
                                    'fallbackType',
                                    'callback',
                                    'originalUrl',

                                    // данные для show
                                    'title',
                                    'displayUrl',
                                    // для подмены домена в ПП и Ябро
                                    'keyUrl',
                                    'displayHost',

                                    // данные для навигации
                                    'from',
                                    'to',

                                    // данные для крестика и кнопки назад
                                    'depth',

                                    // данные для хуков, которые могут превентить события
                                    'preventDefault',

                                    // гарды в истории
                                    'intent',
                                    'type',

                                    // данные post-message
                                    'orig',
                                    'url',
                                    'cleanUrl',
                                    'fixSwipe',
                                    'action',
                                    'class',
                                    'fromUrl',
                                    'package',
                                    'originalUrl',
                                    'url',
                                    'host',
                                    'displayHost',
                                    'displayUrl',

                                    // состояние жестов в post-message
                                    'movement',
                                    'delta',
                                    'first',
                                    'last',
                                    'velocity',
                                    'x',
                                    'y',
                                    'elapsedTime',
                                    'startTime',
                                    'initialPosition',
                                    'probeStatus',

                                    // параметры
                                    'itd',
                                    'prefetch',
                                    'event-id',
                                    'parent-reqid',
                                    'trbsrc',
                                    'new_overlay',
                                    'check_swipe',
                                    'no_bolver',

                                    // hermione cgi параметры котика
                                    'srcrwr',
                                    'renderer_export',

                                    // конфиг
                                    'openTurboApp',
                                    'openIos12InNewTab',

                                    // варианты внешнего вида
                                    // чтобы не минифицировалось EViewType
                                    'classic',
                                    'drawer',
                                ],
                                // не меняем названия модулей
                                regex: /.{5,}/,
                            },
                        },
                    },
                    parallel: true,
                }),
            ],
        },

        plugins: [
            new CssoWebpackPlugin(),
            new webpackPluginHashOutput({
                validateOutput: true,
                validateOutputRegex: /hashed[\s\S]+\.js$/,
            }),
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
    },
);
