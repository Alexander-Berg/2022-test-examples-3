const {assert} = require('chai');
const Asker = require('vow-asker');

const resource = require('./resources/setup');
const httpTest = require('./lib/http');

const INVALID_XML = '<ololo></ololo></ololo>';

module.exports = {
    'errors: invalid xml in mandatory resource': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(INVALID_XML);
        });

        return resource('simple', null, {port: server.port, dataType: 'xml', isMandatory: true})
            .fail(function (e) {
                assert.strictEqual(e.code, resource.Error.CODES.INVALID_XML);
                assert.strictEqual(
                    e.message,
                    `${'Invalid XML <simple.get> '
                        + 'Error: Unmatched closing tag: ololo'
                        + '\nLine: 0'
                        + '\nColumn: 23'
                        + '\nChar: > http://127.0.0.1:'}${server.port}/test: ${INVALID_XML}`
                );

                done();
            })
            .done();
    }),
    'errors: invalid json in mandatory resource': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(INVALID_XML);
        });

        return resource('simple', null, {port: server.port, isMandatory: true})
            .fail(function (e) {
                assert.strictEqual(e.code, resource.Error.CODES.INVALID_JSON);
                assert.strictEqual(
                    e.message,
                    `${'Invalid JSON <simple.get> SyntaxError: Unexpected token'
                        + ' < in JSON at position 0 http://127.0.0.1:'}${
                        server.port}/test: ${INVALID_XML}`
                );

                done();
            })
            .done();
    }),
    'errors: invalid xml in non-mandatory resource': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(INVALID_XML);
        });

        return resource('simple', null, {port: server.port, dataType: 'xml'})
            .then(function (data) {
                assert.strictEqual(data, null);

                done();
            })
            .done();
    }),
    'errors: unknown resource': function (done) {
        return resource('lol')
            .fail(function (e) {
                assert.strictEqual(e.code, resource.Error.CODES.ERROR_REQUIRING_RESOURCE);

                done();
            })
            .done();
    },
    'errors: unknown method in mandatory resource': function (done) {
        return resource('simple.put', null, {isMandatory: true})
            .fail(function (e) {
                assert.strictEqual(e.code, resource.Error.CODES.UNKNOWN_METHOD);

                done();
            })
            .done();
    },
    'errors: unexpected result in mandatory resource': function (done) {
        return resource('unexpected-result', null, {isMandatory: true})
            .fail(function (e) {
                assert.strictEqual(e.code, resource.Error.CODES.UNEXPECTED_RESULT);
                assert.strictEqual(e.message,
                    'Unexpected result <unexpected-result.get> {"some":"trololo"} example.com');

                done();
            })
            .done();
    },
    'errors: invalid-arguments in mandatory resource': function (done) {
        return resource('invalid-arguments', null, {isMandatory: true})
            .fail(function (e) {
                assert.strictEqual(e.code, resource.Error.CODES.INVALID_ARGUMENTS);
                assert.strictEqual(e.message, 'Invalid method arguments <invalid-arguments.get> example.com');

                done();
            })
            .done();
    },
    'errors: unsufficient status code in mandatory resource': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 500;
            res.end();
        });

        return resource('simple', null, {port: server.port, isMandatory: true})
            .fail(function (e) {
                assert.strictEqual(e.code, Asker.Error.CODES.UNEXPECTED_STATUS_CODE);

                done();
            })
            .done();
    }),
    'errors: succesful retry': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 500;
            res.end();
        });

        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({result: 'ok'}));
        });

        return resource('with-meta', null, {port: server.port, maxRetries: 2})
            .then(function (data) {
                assert.strictEqual(data._meta.options.maxRetries, 2);

                done();
            })
            .done();
    }),
};
