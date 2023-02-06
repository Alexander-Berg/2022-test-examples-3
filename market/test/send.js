const {assert} = require('chai');

const resource = require('./resources/setup');
const httpTest = require('./lib/http');

const TEXT_RESPONSE = 'my cool string';
const BODY_MULTIPART = {
    simple_param: 'hey!',
    complex_param: {
        key1: 'one',
        key2: 'two',
    },
};
const TEST_BUFFER = Buffer.from(TEXT_RESPONSE);

module.exports = {
    'send: put with multipart': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (
                req.method === 'PUT' && req.body
                && JSON.parse(req.body.complex_param).key1 === BODY_MULTIPART.complex_param.key1
            ) {
                res.statusCode = 201;
                res.end(TEXT_RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        return resource('send.multipart', {body: BODY_MULTIPART}, {port: server.port})
            .then(function (data) {
                assert.strictEqual(data, TEXT_RESPONSE);

                done();
            })
            .done();
    }),
    'send: put with string': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.method === 'PUT' && req.body && req.body === TEXT_RESPONSE) {
                res.statusCode = 201;
                res.end(TEXT_RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        return resource('send.string', {body: TEXT_RESPONSE}, {port: server.port})
            .then(function (data) {
                assert.strictEqual(data, TEXT_RESPONSE);

                done();
            })
            .done();
    }),
    'send: put with buffer': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.method === 'PUT' && req.body && req.body === TEXT_RESPONSE) {
                res.statusCode = 201;
                res.end(TEXT_RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        return resource('send.buffer', {body: TEST_BUFFER}, {port: server.port})
            .then(function (data) {
                assert.strictEqual(data, TEXT_RESPONSE);

                done();
            })
            .done();
    }),
};
