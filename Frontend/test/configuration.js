/* eslint-disable import/no-extraneous-dependencies */

let url = require('url');
let assert = require('chai').assert;
let extend = require('extend');
let resource = require('./resources/setup');
let httpTest = require('./lib/http');
let params = {
    locale: 'ru',
    lang: 'ru',
};

module.exports = {
    'configuration: env + static, no redifinition': httpTest(function(done, server) {
        server.addTest(function(req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify(url.parse(req.url, true).query));
        });

        return resource('with-meta', params, { port: server.port })
            .then(function(data) {
                let info = data._meta.options;

                assert.deepEqual(data.locale, 'ru');
                assert.deepEqual(data.lang, 'ru');

                assert.strictEqual(info.host, 'localhost');
                assert.strictEqual(info.port, server.port);
                assert.strictEqual(info.path, '/test?locale=ru&lang=ru');
                assert.strictEqual(info.maxRetries, 1);
                assert.strictEqual(info.timeout, 300);

                done();
            })
            .done();
    }),
    'configuration: env + static, redifinition on invoke': httpTest(function(done, server) {
        server.addTest(function(req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify(url.parse(req.url, true).query));
        });

        return resource('with-meta', params, { port: server.port, timeout: 500 })
            .then(function(data) {
                assert.deepEqual(data.locale, 'ru');
                assert.deepEqual(data.lang, 'ru');

                assert.strictEqual(data._meta.options.timeout, 500);

                done();
            })
            .done();
    }),
    'configuration: any Asker option should be configurable': httpTest(function(done, server) {
        let HEADERS = {
            'x-trololo': 'Ololo',
        };
        let BODY = new Buffer('some test data');
        let METHOD = 'POST';
        let BODY_ENCODING = 'raw';
        let statusFilter = function(code) {
            return {
                /* jshint bitwise: false */
                accept: [200, 201, 404].indexOf(code) !== -1,
                isRetryAllowed: true,
            };
        };

        server.addTest(function(req, res) {
            if (req.method === METHOD) {
                res.statusCode = 404;
                res.end(JSON.stringify({ response: req.body }));
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        return resource('with-meta', null,
            {
                port: server.port,
                method: METHOD,
                headers: HEADERS,
                body: BODY,
                bodyEncoding: BODY_ENCODING,
                statusFilter: statusFilter,
            },
        )
            .then(function(data) {
                let askerOptions = data._meta.options;

                assert.strictEqual(data.response, BODY.toString());
                assert.strictEqual(askerOptions.body, BODY);
                assert.strictEqual(askerOptions.method, METHOD);
                assert.strictEqual(askerOptions.bodyEncoding, BODY_ENCODING);

                // Asker sets gzip by default and adds content-length for raw body
                assert.deepEqual(
                    askerOptions.headers,
                    extend(HEADERS, { 'accept-encoding': 'gzip, *', 'content-length': BODY.toString().length }),
                );

                done();
            })
            .done();
    }),
    'configuration: options not recognized by Asker should not pass': httpTest(function(done, server) {
        server.addTest(function(req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({ response: req.body }));
        });

        return resource('with-meta', null,
            {
                port: server.port,
                blabla: 'blabla',
                trololo: 'trololo',
            },
        )
            .then(function(data) {
                let askerOptions = data._meta.options;

                assert.strictEqual(askerOptions.blabla, undefined);
                assert.strictEqual(askerOptions.trololo, undefined);

                done();
            })
            .done();
    }),
};
