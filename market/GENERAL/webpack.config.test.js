'use strict';

const commonConfig = require('tsum-front-core/webpack.config.common');

module.exports = commonConfig(__dirname, {env: 'test'});
