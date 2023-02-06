const {assert} = require('chai');

const httpTest = require('./lib/http');
const Asker = require('../lib/asker');

const ask = Asker;

module.exports = {
    'execute request without callback': httpTest(function (done, server) {
        const PATH = '/test';

        server.addTest(function (req, res) {
            assert.strictEqual(req.url, PATH,
                'request recieved');

            res.end();

            done();
        });

        ask({port: server.port, path: PATH});
    }),
};
