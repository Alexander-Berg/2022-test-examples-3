'use strict';

const mockery = require('mockery');

const lruCache = function (data) {
    return {
        has: () => false,
        set: () => ({}),
        get: () => data
    };
};

module.exports = function mockCache(data) {
    mockery.registerMock('lru-cache', lruCache.bind(lruCache, data));

    mockery.enable({
        useCleanCache: true,
        warnOnReplace: false,
        warnOnUnregistered: false
    });
};
