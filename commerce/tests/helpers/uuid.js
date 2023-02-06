'use strict';

const mockery = require('mockery');

const v4 = function () {
    return 'some-random-uuid';
};

module.exports = function mockMailer() {
    mockery.registerMock('uuid', { v4 });
    mockery.enable({
        useCleanCache: true,
        warnOnReplace: false,
        warnOnUnregistered: false
    });
};
