const { merge } = require('lodash');
const production = require('./../production/borschik');
const { STATIC_HOST } = require('./../../tools/constants');

const testing = {
    paths: {
        './': `${STATIC_HOST}`,
        './freeze/': `${STATIC_HOST}freeze/`
    }
};

module.exports = merge(production, testing);
