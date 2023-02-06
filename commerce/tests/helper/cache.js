const _ = require('lodash');
const mockery = require('mockery');

const defaultHandlers = {
    has: () => false,
    get: () => ({}),
    set: () => ({}),
    del: () => ({})
};

const mockCache = (handlers = {}) => {
    const pickedHandlers = _.pick(handlers, _.keys(defaultHandlers));
    const mock = _.assign(defaultHandlers, pickedHandlers);

    mockery.registerMock('lru-cache', () => mock);

    mockery.enable({
        useCleanCache: true,
        warnOnReplace: false,
        warnOnUnregistered: false
    });
};

module.exports = mockCache;
