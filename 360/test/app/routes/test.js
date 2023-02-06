'use strict';

module.exports.global = true;

module.exports.routes = [ {
    name: '/',
    path: require.resolve('../handlers/log.js')
}, {
    name: '/ping',
    path: require.resolve('../handlers/ping.js')
}, {
    name: '/exception',
    path: require.resolve('../handlers/exception.js')
} ];
