'use strict';

const baseConfig = require('../../.hermione.conf');

baseConfig.plugins = {
    'hermione-passive-browsers': baseConfig.plugins['hermione-passive-browsers']
};

module.exports = baseConfig;
