const fs = require('fs');

const {assert} = require('chai');

const Asker = require('../lib/asker');
const httpTest = require('./lib/http');

const ask = Asker;

const RESPONSE = 'response ok';
const filePath = 'test/pic.jpg';
const file2Path = 'test/pic2.jpeg';
const fileSize = fs.statSync(filePath).size;
const fileBuffer = fs.readFileSync(filePath);
const file2Buffer = fs.readFileSync(file2Path);
const bodyText = 'plain text test';
const bodyStringify = {
    data: [
        {id: 'AC', name: 'AC'},
        {id: 'ACURA', name: 'Acura'},
        {id: 'ALFA_ROMEO', name: 'Alfa Romeo'},
        {id: 'ALPINA', name: 'Alpina'},
        {id: 'ARO', name: 'Aro'},
    ],
};
const bodyUrlencoded = {
    simple: 'ok',
    complex: [
        'one',
        'two',
    ],
};
const bodyMultipart = {
    simple_param: 'hey!',
    non_string_literal: 32,
    complex_param: {
        key1: 'one',
        key2: 'two',
    },
    file0: fileBuffer,
    file1: {
        filename: 'pic1.jpg',
        mime: 'image/jpeg',
        data: fileBuffer,
    },
    multiple_files: [
        {
            filename: 'pic1.jpg',
            mime: 'image/jpeg',
            data: fileBuffer,
        },
        {
            filename: 'pic2.jpg',
            mime: 'image/jpeg',
            data: file2Buffer,
        },
    ],
};

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
    'body encoder "raw"': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = isBuffersContentEqual(req.body, fileBuffer) ? 200 : 500;
            res.end();
        });

        ask({port: server.port, method: 'post', bodyEncoding: 'raw', body: fileBuffer},
            function (error, response) {
                assert.strictEqual(response.statusCode, 200);
                done();
            });
    }),

    'body encoder "raw" shoudl throw an error if `body` is not a Buffer': httpTest(function (done, server) {
        try {
            ask({port: server.port, method: 'post', bodyEncoding: 'raw', body: 'not a buffer'},
                function () {});
        } catch (error) {
            assert.strictEqual(error.code, Asker.Error.CODES.UNEXPECTED_BODY_TYPE);
            done();
        }
    }),

    'body encoder "string"': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.body && req.body.toString() === bodyText) {
                res.statusCode = 200;
                res.end(RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        ask({
            port: server.port,
            method: 'post',
            bodyEncoding: 'string',
            body: bodyText,
        }, function (error, response) {
            assert.isNull(error, 'no errors occured');
            assert.strictEqual(response.data.toString(), RESPONSE, 'response is correct');

            done();
        });
    }),

    'body encoder "json"': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.body && req.body.data[0].id === bodyStringify.data[0].id) {
                res.statusCode = 200;
                res.end(RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        ask({
            port: server.port,
            method: 'post',
            bodyEncoding: 'json',
            body: bodyStringify,
        }, function (error, response) {
            assert.isNull(error, 'no errors occured');
            assert.strictEqual(response.data.toString(), RESPONSE, 'response is correct');

            done();
        });
    }),

    'body encoder "urlencoded"': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.body && req.body.simple === bodyUrlencoded.simple) {
                res.statusCode = 200;
                res.end(RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        ask({
            port: server.port,
            method: 'post',
            bodyEncoding: 'urlencoded',
            body: bodyUrlencoded,
        }, function (error, response) {
            assert.isNull(error, 'no errors occured');
            assert.strictEqual(response.data.toString(), RESPONSE, 'response is correct');

            done();
        });
    }),

    'test body encoder "urlencoded" => incorrect body type': function () {
        assert.throw(function () {
            ask({bodyEncoding: 'urlencoded', body: bodyText});
        }, /Unexpected type ".*" of the option "body" in the body encoder "urlencoded". Expected \{Object\}/);
    },

    'body encoder "multipart"': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            const {body} = req;
            const {files} = req;
            let complexParam;

            assert.doesNotThrow(function () {
                complexParam = JSON.parse(body.complex_param);
            });
            assert.strictEqual(complexParam.key1, bodyMultipart.complex_param.key1);
            assert.strictEqual(body.non_string_literal, String(bodyMultipart.non_string_literal));
            assert.strictEqual(files.file1.size, fileSize);

            assert.isArray(files.multiple_files);
            assert.strictEqual(files.multiple_files.length, bodyMultipart.multiple_files.length);

            // @note: formidable doesn't guaranty order of multipart form processing, so convert them to collection
            // @see https://github.com/felixge/node-formidable/issues/261
            function filesToCollection(files) {
                return files.reduce(function (res, file) {
                    const filename = file.name || file.filename;
                    res[filename] = {
                        filename,
                        mime: file.type || file.mime,
                        data: file.path ? fs.readFileSync(file.path) : file.data,
                    };
                    return res;
                }, {});
            }
            assert.deepEqual(
                filesToCollection(files.multiple_files),
                filesToCollection(bodyMultipart.multiple_files)
            );

            assert.ok(isBuffersContentEqual(fs.readFileSync(files.file0.path), bodyMultipart.file0));

            res.statusCode = 200;
            res.end(RESPONSE);
        });

        ask({
            port: server.port,
            method: 'post',
            bodyEncoding: 'multipart',
            body: bodyMultipart,
        }, function (error, response) {
            assert.isNull(error, 'no errors occured');
            assert.strictEqual(response.data.toString(), RESPONSE, 'response is correct');

            done();
        });
    }),

    'throw error on incorrect value of the option "bodyEncoding"': function () {
        assert.throw(function () {
            // eslint-disable-next-line no-new
            new Asker({bodyEncoding: 'duckduckandsend', body: bodyUrlencoded});
        }, /Body encoder ".*" is not defined/);
    },

    'test body encoder "multipart" => incorrect body type': function () {
        assert.throw(function () {
            ask({bodyEncoding: 'multipart', body: bodyText});
        }, /Unexpected type ".*" of the option "body" in the body encoder "multipart". Expected \{Object\}/);
    },

    'test body encoder => manually set content-type': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            res.statusCode = 200;
            res.end(RESPONSE);
        });

        ask({
            port: server.port,
            method: 'post',
            headers: {'content-type': 'text/plain'},
            body: bodyText,
        }, function (error, response) {
            assert.strictEqual(error, null, 'no errors occured');
            assert.strictEqual(response.data.toString(), RESPONSE, 'response is correct');

            done();
        });
    }),

    'test body encoder => no file data for multipart': httpTest(function (done, server) {
        server.addTest(function (req, res) {
            if (req.body && JSON.parse(req.body.file).filename === 'nope') {
                res.statusCode = 200;
                res.end(RESPONSE);
            } else {
                res.statusCode = 500;
                res.end();
            }
        });

        ask({
            port: server.port,
            method: 'post',
            bodyEncoding: 'multipart',
            body: {file: {filename: 'nope'}},
        }, function (error, response) {
            assert.strictEqual(error, null, 'no errors occured');
            assert.strictEqual(response.data.toString(), RESPONSE, 'response is correct');

            done();
        });
    }),
};
