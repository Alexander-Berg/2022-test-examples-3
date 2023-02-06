'use strict';

const mockery = require('mockery');

class success {
    constructor(response) {
        this.response = response;
    }

    putObject(_, cb) {
        return cb(false, this.response);
    }
}

class retry {
    constructor(response) {
        this.isError = false;
        this.response = response;
    }

    putObject(_, cb) {
        this.isError = !this.isError;

        cb(this.isError, this.response);
    }
}

class fail {
    constructor(response) {
        this.response = response;
    }

    putObject(_, cb) {
        cb('Some error');
    }
}

const mocks = { success, retry, fail };

module.exports = (response, type = 'success') => {
    const S3 = mocks[type].bind(null, response);

    mockery.registerMock('aws-sdk', { S3 });

    mockery.enable({
        useCleanCache: true,
        warnOnReplace: false,
        warnOnUnregistered: false
    });

    return require('models/s3');
};
