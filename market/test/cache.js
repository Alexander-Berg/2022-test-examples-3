const {assert} = require('chai');

const resource = require('./resources/setup');
const httpTest = require('./lib/http');

module.exports = {
    'cache: base resource class is not affected': function (done) {
        assert.isUndefined(resource.cache);

        done();
    },
    'cache: get': function (done) {
        return resource('cache', null, {cache: {get: true}})
            .then(function (data) {
                assert.strictEqual(data.key, '__methodName=get&__resourceName=cache&');

                done();
            })
            .done();
    },
    'cache: get with generation': function (done) {
        return resource('cache',
            {test: 'test'},
            {
                cache: {
                    get: {keyTTL: 1000},
                    generation: 2,
                },
            })
            .then(function (data) {
                assert.strictEqual(data.key, 'v2:__methodName=get&__resourceName=cache&test=test&');

                done();
            })
            .done();
    },
    'cache: failed': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({some: 'data'}));
        });

        return resource('cache', {problem: 'tada'}, {port: server.port})
            .then(function (data) {
                assert.strictEqual(data.some, 'data');

                done();
            })
            .done();
    }),
    'cache: disabled': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(JSON.stringify({some: 'data'}));
        });

        return resource('cache', null, {port: server.port, cache: false})
            .then(function (data) {
                assert.strictEqual(data.some, 'data');

                done();
            })
            .done();
    }),
};
