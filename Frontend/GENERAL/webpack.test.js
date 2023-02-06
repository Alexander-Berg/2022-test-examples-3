const path = require('path');
const { merge } = require('webpack-merge');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

const { htmlConfig: _htmlConfig, loaderConfig: _loaderConfig } = require('./webpack.common.js');
const ReplacerPlugin = require('./webpack/plugins/replacer.js');
const postcssOptions = require('./postcss.config');

const htmlConfig = merge(_htmlConfig, {
  mode: 'none',
  output: {
    ..._htmlConfig.output,
    path: path.resolve(__dirname, './hermione'),
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [
          {
            loader: MiniCssExtractPlugin.loader,
          },
          {
            loader: 'css-loader',
            options: {
              importLoaders: 1,
            },
          },
          {
            loader: 'postcss-loader',
            options: postcssOptions,
          },
        ],
      },
    ],
  },
});

htmlConfig.plugins = [
  ...htmlConfig.plugins.map((plugin) => {
    if (!(plugin instanceof HtmlWebpackPlugin)) {
      return plugin;
    }

    const filename = plugin.userOptions.filename;

    if (filename.includes('advanced')) {
      // Hide image in hermione tests
      plugin.userOptions.styles += `
        .AdvancedCaptcha-Image {
          opacity: 0;
        }
      `;
    } else if (filename.includes('checkbox')) {
      const script = plugin.userOptions.scriptHead || '';

      // Mock window.PGreed
      plugin.userOptions.scriptHead =
        script +
        `
        window.PGreed = {
          safeGet: function safeGet() {
            return new Promise(function fingerprint(resolve) {
              resolve('fingerprint');
            });
          }
        };
      `;
    }

    return plugin;
  }),
  new webpack.DefinePlugin({
    'process.env.NODE_ENV': '"testing"',
  }),
];

const loaderConfig = merge(_loaderConfig, {
  mode: 'none',
  output: {
    ..._loaderConfig.output,
    path: path.resolve(__dirname, './hermione/'),
  },
});

loaderConfig.plugins = [
  ...loaderConfig.plugins.map((plugin) => {
    if (plugin instanceof ReplacerPlugin) {
      plugin.options.manifestPath = [path.join(__dirname, './hermione/html-manifest.json')];
    }

    return plugin;
  }),
  new webpack.DefinePlugin({
    'process.env.NODE_ENV': '"testing"',
  }),
];

module.exports = [htmlConfig, loaderConfig];
