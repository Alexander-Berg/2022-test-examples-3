'use strict';

const getServiceOptionsFactory = require('../../helpers/get-service-options-factory.js');
const prodOptionsFactory = require('../production');

module.exports = getServiceOptionsFactory(prodOptionsFactory(), __dirname);
