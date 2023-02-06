const {assert} = require('chai');

const ask = require('../lib/asker');
const httpTest = require('./lib/http');
const httpsTest = require('./lib/https');

module.exports = {
    'http: check response format': httpTest(function (done, server) {
        const RESPONSE = 'response ok';

        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(RESPONSE);
        });

        ask({port: server.port}, function (error, response) {
            assert.strictEqual(error, null, 'no errors occurred');

            assert.strictEqual(response.data.toString(), RESPONSE, 'response data is present and is a correct string');

            assert.strictEqual(response.statusCode, 200, 'statusCode is present and equals 200');

            assert(response.meta.time.network >= 0, 'network time is present and is a number');
            assert(response.meta.time.total >= 0, 'total time is present and is a number');
            assert(response.meta.time.total >= response.meta.time.network,
                'total time is greater or equals total time');

            assert.strictEqual(response.meta.retries.used, 0, 'no retries were used');
            assert.strictEqual(response.meta.retries.limit, 0, 'no retries allowed by default');

            assert.strictEqual(response.meta.options.port, server.port, 'port is correct');

            done();
        });
    }),

    'https: check response format': httpsTest(function (done, server) {
        const RESPONSE = 'response ok';

        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(RESPONSE);
        });

        ask({protocol: server.protocol, port: server.port, ca: server.rootCA}, function (error, response) {
            assert.strictEqual(error, null, 'no errors occurred');

            assert.strictEqual(response.data.toString(), RESPONSE, 'response data is present and is a correct string');

            assert.strictEqual(response.statusCode, 200, 'statusCode is present and equals 200');

            assert(response.meta.time.network >= 0, 'network time is present and is a number');
            assert(response.meta.time.total >= 0, 'total time is present and is a number');
            assert(response.meta.time.total >= response.meta.time.network,
                'total time is greater or equals total time');

            assert.strictEqual(response.meta.retries.used, 0, 'no retries were used');
            assert.strictEqual(response.meta.retries.limit, 0, 'no retries allowed by default');

            assert.strictEqual(response.meta.options.port, server.port, 'port is correct');

            done();
        });
    }),

    'check empty response body': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 201;
            res.end('');
        });

        ask({port: server.port}, function (error, response) {
            assert.strictEqual(error, null, 'no errors occured');
            assert.strictEqual(response.statusCode, 201, 'statusCode equals 201');
            assert.strictEqual(response.data, null, 'response is null');

            done();
        });
    }),
};
