'use strict';

module.exports = {
    path: require.resolve('./vdirect-keys.txt')
};

require.cache[require.resolve('../../lib/vdirect-config.js')] = module;
