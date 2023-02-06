const {mergeDeepRight} = require('ramda');

const config = require('../../ginny/partner.touch');
const commonConfig = require('./partner.common.conf');

module.exports = mergeDeepRight(config, commonConfig);
