const { merge } = require('webpack-merge');

const prodConfig = require('./webpack.prod');

module.exports = merge(prodConfig, {
  devtool: 'cheap-module-source-map',
});
