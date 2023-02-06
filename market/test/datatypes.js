const {assert} = require('chai');

const resource = require('./resources/setup');
const httpTest = require('./lib/http');

const TEXT_RESPONSE = 'my cool string';
const XML_RESPONSE = `<root><trololo>  ${TEXT_RESPONSE}  </trololo></root>`;
const RAW_RESPONSE = Buffer.from(TEXT_RESPONSE);

function isBuffersContentEqual(b1, b2) {
    if (!Buffer.isBuffer(b1) || !Buffer.isBuffer(b2) || b1.length !== b2.length) {
        return false;
    }

    for (let i = 0; i < b1.length; i++) {
        if (b1[i] !== b2[i]) {
            return false;
        }
    }

    return true;
}

module.exports = {
    'datatypes: raw': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(RAW_RESPONSE);
        });

        return resource('simple', null, {port: server.port, dataType: 'raw'})
            .then(function (data) {
                assert(Buffer.isBuffer(data));
                assert(isBuffersContentEqual(data, RAW_RESPONSE));

                done();
            })
            .done();
    }),
    'datatypes: text': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(TEXT_RESPONSE);
        });

        return resource('simple', null, {port: server.port, dataType: 'text'})
            .then(function (data) {
                assert.strictEqual(data, TEXT_RESPONSE);

                done();
            })
            .done();
    }),
    'datatypes: xml with buildPath': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(XML_RESPONSE);
        });

        return resource(
            'simple',
            null,
            {port: server.port, dataType: 'xml', xml: {buildPath: 'root/trololo/text()'}}
        )
            .then(function (xml) {
                assert.strictEqual(xml.eq(0), TEXT_RESPONSE);

                done();
            })
            .done();
    }),
    'datatypes: xml with buildPath and disabled trim': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(XML_RESPONSE);
        });

        return resource(
            'simple',
            null,
            {port: server.port, dataType: 'xml', xml: {buildPath: 'root/trololo/text()', trim: false}}
        )
            .then(function (xml) {
                assert.strictEqual(xml.eq(0), `  ${TEXT_RESPONSE}  `);

                done();
            })
            .done();
    }),
};
