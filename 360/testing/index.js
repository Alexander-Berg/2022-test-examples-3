'use strict';

const buildServiceOptions = require('@ps-int/mail-lib').serviceConfigs.helpers['build-service-options'];
const baseOptions = require('../base');

module.exports = buildServiceOptions(baseOptions, __dirname);
