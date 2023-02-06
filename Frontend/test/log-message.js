/* eslint-disable import/no-extraneous-dependencies */

let TEST_LOG_ID = 'test';
let instance = new resource({
    logId: TEST_LOG_ID,
});
let assert = require('chai').assert;
let extend = require('extend');
let resource = require('./resources/setup');
let META_MOCK = {
    cache: true,
    time: {
        network: 200,
        total: 250,
    },
    retries: {
        used: 1,
        limit: 2,
    },
};

module.exports = {
    'logMessage: cache and retries': function() {
        assert.strictEqual(instance.getLogMessage(META_MOCK),
            'Resolved request <' + TEST_LOG_ID + '> [in 250 ms] [from cache] [retry: 1]');
    },
    'logMessage: no cache and no retries': function() {
        instance._requestOptions = {
            body: 'string',
        };

        let META_MOCK_SHORT = extend({}, META_MOCK, {
            cache: false,
            retries: {
                used: 0,
            },
        });

        assert.strictEqual(instance.getLogMessage(META_MOCK_SHORT),
            'Resolved request <' + TEST_LOG_ID + '> [in 200~250 ms] for localhost:80 string');
    },
    'logMessage: buildUrl with object body': function() {
        instance._requestOptions = {
            body: META_MOCK,
        };

        assert.strictEqual(instance.buildURL(), 'localhost:80 ' + JSON.stringify(META_MOCK));
    },
    'logMessage: buildUrl with string body': function() {
        instance._requestOptions = {
            body: 'string',
        };

        assert.strictEqual(instance.buildURL(), 'localhost:80 string');
    },
    'logMessage: buildUrl with buffer body': function() {
        instance._requestOptions = {
            body: new Buffer('string'),
        };

        assert.strictEqual(instance.buildURL(), 'localhost:80 <Buffer size:6>');
    },
    'logMessage: check url and resource name in getCommonErrorData': function() {
        let errorData;

        instance._requestOptions = {
            body: META_MOCK,
        };

        errorData = instance.getCommonErrorData();

        assert.strictEqual(errorData.url, 'localhost:80 ' + JSON.stringify(META_MOCK));
        assert.strictEqual(errorData.resource, TEST_LOG_ID);
    },
};
