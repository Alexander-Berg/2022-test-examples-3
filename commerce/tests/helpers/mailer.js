'use strict';

const mockery = require('mockery');

const mailerMock = function () {
    return new Promise(resolve => {
        resolve();
    });
};

module.exports = function mockMailer() {
    mockery.registerMock('helpers/mailer', mailerMock);
    mockery.enable({
        useCleanCache: true,
        warnOnReplace: false,
        warnOnUnregistered: false
    });
};
