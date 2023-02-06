const fs = require('fs');
const url = require('url');

const {assert} = require('chai');
const assign = require('object-assign');

const resource = require('./resources/setup');
const httpTest = require('./lib/http');

const myRootCA = fs.readFileSync(`${__dirname}/resources/certs/myRootCA.pem`);

const params = {
    locale: 'ru',
    lang: 'ru',
};

module.exports = {
    'configuration: env + static, no redifinition': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify(url.parse(req.url, true).query));
        });

        return resource('with-meta', params, {port: server.port})
            .then(function (data) {
                const info = data._meta.options;

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
    'configuration: env + static, redifinition on invoke': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify(url.parse(req.url, true).query));
        });

        return resource('with-meta', params, {port: server.port, timeout: 500})
            .then(function (data) {
                assert.deepEqual(data.locale, 'ru');
                assert.deepEqual(data.lang, 'ru');

                assert.strictEqual(data._meta.options.timeout, 500);

                done();
            })
            .done();
    }),
    'configuration: any Asker option should be configurable': httpTest(function (done, server) {
        const HEADERS = {
            'x-trololo': 'Ololo',
        };
        const BODY = Buffer.from('some test data');
        const METHOD = 'POST';
        const BODY_ENCODING = 'raw';
        const isNetworkError = function (code) {
            return [200, 201, 404].indexOf(code) === -1;
        };
        const isRetryAllowed = function () {
            return true;
        };

        server.addTest(function (req, res) {
            if (req.method === METHOD) {
                res.statusCode = 404;
                res.end(JSON.stringify({response: req.body}));
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        const opts = {
            port: server.port,
            method: METHOD,
            headers: HEADERS,
            body: BODY,
            bodyEncoding: BODY_ENCODING,
            isNetworkError,
            isRetryAllowed,
        };

        return resource('with-meta', null, opts)
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.strictEqual(data.response, BODY.toString());
                assert.strictEqual(askerOptions.body, BODY);
                assert.strictEqual(askerOptions.method, METHOD);
                assert.strictEqual(askerOptions.bodyEncoding, BODY_ENCODING);

                // Asker sets gzip by default and adds content-length for raw body
                assert.deepEqual(
                    askerOptions.headers,
                    assign(HEADERS, {'accept-encoding': 'gzip, *', 'content-length': BODY.toString().length})
                );
            })
            .done(done);
    }),
    'configuration: status filter should be configurable': httpTest(function (done, server) {
        const HEADERS = {
            'x-trololo': 'Ololo',
        };
        const BODY = Buffer.from('some test data');
        const METHOD = 'POST';
        const BODY_ENCODING = 'raw';
        const statusFilter = function (code) {
            return {
                // eslint-disable-next-line no-bitwise
                accept: ~[200, 201, 404].indexOf(code),
                isRetryAllowed: true,
            };
        };

        server.addTest(function (req, res) {
            if (req.method === METHOD) {
                res.statusCode = 404;
                res.end(JSON.stringify({response: req.body}));
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        const opts = {
            port: server.port,
            method: METHOD,
            headers: HEADERS,
            body: BODY,
            bodyEncoding: BODY_ENCODING,
            statusFilter,
        };

        return resource('with-meta', null, opts)
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.strictEqual(data.response, BODY.toString());
                assert.strictEqual(askerOptions.body, BODY);
                assert.strictEqual(askerOptions.method, METHOD);
                assert.strictEqual(askerOptions.bodyEncoding, BODY_ENCODING);

                // Asker sets gzip by default and adds content-length for raw body
                assert.deepEqual(
                    askerOptions.headers,
                    assign(HEADERS, {'accept-encoding': 'gzip, *', 'content-length': BODY.toString().length})
                );
            })
            .done(done);
    }),
    'configuration: default status filter should pass 304 status code': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 304;
            res.end();
        });

        return resource('with-meta', null, {port: server.port, isMandatory: true})
            .then(function (data) {
                assert.strictEqual(data._meta.statusCode, 304);
            })
            .done(done);
    }),
    'configuration: status filter should be configurable with resource config': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 403;
            res.end();
        });

        return resource('cfg-status-filter', null, {port: server.port, isMandatory: true})
            .then(function (data) {
                assert.strictEqual(data._meta.statusCode, 403);
            })
            .done(done);
    }),
    'configuration: isNetworkError should be configurable with resource config': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 403;
            res.end();
        });

        return resource('cfg-network-error', null, {port: server.port, isMandatory: true})
            .then(function (data) {
                assert.strictEqual(data._meta.statusCode, 403);
            })
            .done(done);
    }),
    'configuration: options not recognized by Asker should not pass': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({response: req.body}));
        });

        const opts = {
            port: server.port,
            blabla: 'blabla',
            trololo: 'trololo',
        };

        return resource('with-meta', null, opts)
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.strictEqual(askerOptions.blabla, undefined);
                assert.strictEqual(askerOptions.trololo, undefined);

                done();
            })
            .done();
    }),
    'configuration: authority certificate might be passed with resource config': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end();
        });

        return resource('with-meta', null, {port: server.port, isMandatory: true, ca: myRootCA})
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.isDefined(askerOptions.ca);
                assert.strictEqual(askerOptions.ca.toString(), myRootCA.toString());

                done();
            })
            .done();
    }),
    'configuration: authority certificate path should be resolved from env': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end();
        });

        return resource('with-cert', null, {port: server.port, isMandatory: true})
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.isDefined(askerOptions.ca);
                assert.strictEqual(askerOptions.ca.toString(), myRootCA.toString());

                done();
            })
            .done();
    }),
    'configuration: IP address family might be passed with resource config': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({response: req.body}));
        });

        return resource('with-meta', null, {port: server.port, family: 4})
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.strictEqual(askerOptions.family, 4);
            })
            .done(done);
    }),
    'configuration: define servant param using instance config on call': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({response: req.body}));
        });

        return resource('with-meta', null, {
            port: server.port,
            servant: 'servant',
        })
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.strictEqual(askerOptions.host, '127.0.0.1');
                assert.strictEqual(askerOptions.port, server.port);
                assert.isDefined(askerOptions.ca);
                assert.strictEqual(askerOptions.ca.toString(), myRootCA.toString());

                done();
            })
            .done();
    }),
    'configuration: define servant param in resource constructor': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({response: req.body}));
        });

        return resource('servant-cfg', null, {
            path: '/test',
            port: server.port,
            isMandatory: true,
        })
            .then(function (data) {
                const askerOptions = data._meta.options;

                assert.strictEqual(askerOptions.host, '127.0.0.1');
                assert.strictEqual(askerOptions.port, server.port);
                assert.isDefined(askerOptions.ca);
                assert.strictEqual(askerOptions.ca.toString(), myRootCA.toString());

                done();
            })
            .done();
    }),
};
