const assign = require('object-assign');
const {assert} = require('chai');

const Resource = require('./resources/setup');
const httpTest = require('./lib/http');

const TEST_LOG_ID = 'test';
const instance = new Resource({
    logId: TEST_LOG_ID,
});

const Logger = function () {
    const messages = [];

    this.log = function (msg) {
        messages.push(msg);
    };

    this.getMessages = function () {
        return messages.join('');
    };
};
const META_MOCK = {
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
const DEFAULT_TIMEOUT = 200;

function isErrorLogged(error) {
    // Hacky but easier than using sinon
    return error._isLogged;
}

module.exports = {
    'logMessage: cache and retries': function () {
        assert.strictEqual(instance.getLogMessage(META_MOCK),
            `Resolved request <${TEST_LOG_ID}> [in 250 ms] [from cache] [retry: 1]`);
    },
    'logMessage: no cache and no retries': function () {
        instance._requestOptions = {
            body: 'string',
        };

        const META_MOCK_SHORT = assign({}, META_MOCK, {
            cache: false,
            retries: {
                used: 0,
            },
        });

        assert.strictEqual(instance.getLogMessage(META_MOCK_SHORT),
            `Resolved request <${TEST_LOG_ID}> [in 200~250 ms] for [GET] http://localhost string`);
    },
    'logMessage: buildUrl with object body': function () {
        instance._requestOptions = {
            body: META_MOCK,
        };

        assert.strictEqual(instance.buildURL(), `http://localhost ${JSON.stringify(META_MOCK)}`);
    },
    'logMessage: buildUrl with string body': function () {
        instance._requestOptions = {
            body: 'string',
        };

        assert.strictEqual(instance.buildURL(), 'http://localhost string');
    },
    'logMessage: buildUrl with buffer body': function () {
        instance._requestOptions = {
            body: Buffer.from('string'),
        };

        assert.strictEqual(instance.buildURL(), 'http://localhost <Buffer size:6>');
    },
    'logMessage: buildUrl with buffer body and multiple fields': function () {
        instance._requestOptions = {
            bodyEncoding: 'multipart',
            body: {
                buffer: Buffer.from('string'),
                file: {
                    filename: 'str.txt',
                    mime: 'text/plain',
                    data: Buffer.from('string'),
                },
            },
        };

        assert.strictEqual(
            instance.buildURL(),
            'http://localhost {"buffer":"<Buffer size:6>","file":"<Buffer size:6>"}'
        );
    },
    'logMessage: check url and resource name in getCommonErrorData': function () {
        instance._requestOptions = {
            body: META_MOCK,
        };

        const errorData = instance.getCommonErrorData();

        assert.strictEqual(errorData.url, `http://localhost ${JSON.stringify(META_MOCK)}`);
        assert.strictEqual(errorData.resource, TEST_LOG_ID);
    },
    'logMessage: buildUrl should correctly format custom port for http protocol': function () {
        instance._requestOptions = {
            port: 10080,
        };

        const errorData = instance.getCommonErrorData();

        assert.strictEqual(errorData.url, 'http://localhost:10080');
        assert.strictEqual(errorData.resource, TEST_LOG_ID);
    },
    'logMessage: buildUrl should correctly format custom port for https protocol': function () {
        instance._requestOptions = {
            protocol: 'https:',
            port: 10080,
        };

        const errorData = instance.getCommonErrorData();

        assert.strictEqual(errorData.url, 'https://localhost:10080');
        assert.strictEqual(errorData.resource, TEST_LOG_ID);
    },
    'logMessage: buildUrl should correctly format protocol for https': function () {
        instance._requestOptions = {
            protocol: 'https:',
        };

        const errorData = instance.getCommonErrorData();

        assert.strictEqual(errorData.url, 'https://localhost');
        assert.strictEqual(errorData.resource, TEST_LOG_ID);
    },
    'logMessage: resource.get in log message when call resource without method': function (done) {
        const logger = new Logger();
        const baseLogger = Resource.prototype.logRequest;

        Resource.setRequestLogger(logger.log);

        return Resource('no-http', {please: true, meta: META_MOCK})
            .then(function () {
                assert.strictEqual(logger.getMessages(),
                    'Resolved request <no-http.get> [in 250 ms] [from cache] [retry: 1]');

                Resource.setRequestLogger(baseLogger);

                done();
            })
            .done();
    },
    'logMessage: resource.method in log message': function (done) {
        const logger = new Logger();
        const baseLogger = Resource.prototype.logRequest;

        Resource.setRequestLogger(logger.log);

        return Resource('no-http.advanced', {please: true, meta: META_MOCK})
            .then(function () {
                assert.strictEqual(logger.getMessages(),
                    'Resolved request <no-http.advanced> [in 250 ms] [from cache] [retry: 1]');

                Resource.setRequestLogger(baseLogger);

                done();
            })
            .done();
    },

    'logMessage: does not log timeout error if supressTimeoutError == true is given': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            setTimeout(function () {
                res.end();
            }, DEFAULT_TIMEOUT * 2);
        });

        return Resource('simple', {}, {port: server.port, isMandatory: true, supressTimeoutError: true})
            .fail(function (error) {
                assert.strictEqual(isErrorLogged(error), false);

                done();
            })
            .done();
    }),
    'logMessage: does log other errors even if supressTimeoutError == true is given': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 500;
            res.end();
        });

        return Resource('simple', {supressTimeoutError: true}, {
            port: server.port,
            isMandatory: true,
            supressTimeoutError: true,
        })
            .fail(function (error) {
                assert.strictEqual(isErrorLogged(error), true);

                done();
            })
            .done();
    }),
};
