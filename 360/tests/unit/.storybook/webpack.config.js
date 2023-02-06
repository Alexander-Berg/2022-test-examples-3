const webpack = require('webpack');
const path = require('path');
const staticHost = 'yastatic.net';

module.exports = {
    resolve: {
        modules: [
            path.resolve(__dirname, '../../../components'),
            '../../../node_modules'
        ]
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules\//,
                use: {
                    loader: 'babel-loader?cacheDirectory=true',
                    options: {
                        babelrc: false,
                        presets: ['es2015', 'react']
                    }
                }
            },
            {
                test: /\.styl$/,
                use: [
                    'style-loader',
                    'css-loader',
                    {
                        loader: 'stylus-loader',
                        options: {
                            import: [
                                '@ps-int/ufo-rocks/lib/stylus-vars/colors.styl',
                                '@ps-int/ufo-rocks/lib/stylus-vars/fonts.styl',
                                '@ps-int/ufo-rocks/lib/stylus-vars/layout.styl',
                                '@ps-int/ufo-rocks/lib/stylus-vars/mixins.styl',
                                '@ps-int/ufo-rocks/lib/stylus-vars/utility.styl'
                            ],
                            'resolve url': true,
                            'include css': true,
                            define: {
                                $staticHost: staticHost
                            }
                        }
                    }
                ]
            },
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader']
            },
            {
                test: /\.(svg|jpg)$/,
                use: ['file-loader']
            }
        ]
    },
    plugins: [
        new webpack.DefinePlugin({
            'process.env': {
                BEM_LANG: '"ru"'
            }
        })
    ]
};
