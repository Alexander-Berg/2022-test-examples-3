let assert = require('chai').assert;
let resource = require('./resources/setup');
let httpTest = require('./lib/http');
let TEXT_RESPONSE = 'my cool string';
let XML_RESPONSE = '<root><trololo>  ' + TEXT_RESPONSE + '  </trololo></root>';

module.exports = {
    'datatypes: text': httpTest(function(done, server) {
        server.addTest(function(req, res) {
            res.statusCode = 200;
            res.end(TEXT_RESPONSE);
        });

        return resource('simple', null, { port: server.port, dataType: 'text' })
            .then(function(data) {
                assert.strictEqual(data, TEXT_RESPONSE);

                done();
            })
            .done();
    }),
    'datatypes: xml with buildPath': httpTest(function(done, server) {
        server.addTest(function(req, res) {
            res.statusCode = 200;
            res.end(XML_RESPONSE);
        });

        return resource(
            'simple',
            null,
            { port: server.port, dataType: 'xml', xml: { buildPath: 'root/trololo/text()' } },
        )
            .then(function(xml) {
                assert.strictEqual(xml.eq(0), TEXT_RESPONSE);

                done();
            })
            .done();
    }),
    'datatypes: xml with buildPath and disabled trim': httpTest(function(done, server) {
        server.addTest(function(req, res) {
            res.statusCode = 200;
            res.end(XML_RESPONSE);
        });

        return resource(
            'simple',
            null,
            { port: server.port, dataType: 'xml', xml: { buildPath: 'root/trololo/text()', trim: false } },
        )
            .then(function(xml) {
                assert.strictEqual(xml.eq(0), '  ' + TEXT_RESPONSE + '  ');

                done();
            })
            .done();
    }),
};
