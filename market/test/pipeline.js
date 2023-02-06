const {assert} = require('chai');

const resource = require('./resources/setup');
const httpTest = require('./lib/http');

module.exports = {
    'pipelining: get': function (done) {
        return resource('multiple')
            .then(function (data) {
                assert.isTrue(data.done);

                done();
            })
            .done();
    },
    'pipelining: failed put': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 500;
            res.end();
        });

        return resource('multiple.put', null, {port: server.port})
            .then(function (data) {
                assert.strictEqual(data.ooopsie, 'error');

                done();
            })
            .done();
    }),
    'pipelining: extended': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.url.indexOf('trustworthy') !== -1) {
                res.statusCode = 200;
                res.end(JSON.stringify({success: true}));
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        return resource('multiple.extended', null, {port: server.port})
            .then(function (data) {
                assert.isTrue(data.success);
                assert.strictEqual(data.id, 'service');

                done();
            })
            .done();
    }),
    'pipelining: action': function (done) {
        return resource('multiple.action')
            .then(function (data) {
                assert.strictEqual(data, 'response');

                done();
            })
            .done();
    },
};
