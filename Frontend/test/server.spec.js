const fs = require('fs');
const path = require('path');
const assert = require('assert');
const micro = require('micro');
const request = require('request-promise');
const listen = require('test-listen');
const staticHandler = require('serve-handler');
const pdfRendererHandler = require('../src');

let service;
let staticServer;

let serviceUrl;
let staticPort = 3001;

const getPdfResponse = async(requestDumpName, options = {}) => {
    const response = await request({
        uri: serviceUrl,
        headers: {
            'content-type': 'application/json',
        },
        method: 'POST',
        resolveWithFullResponse: true,
        body: fs.createReadStream(
            path.join(__dirname, 'fixtures/requests', `${requestDumpName}.json`)
        ),
        ...options,
    });

    return response;
};

describe('pdf-renderer', function() {
    before(done => {
        staticServer = micro((req, res) => {
            staticHandler(req, res, {
                public: `${__dirname}/fixtures/pages`,
            });
        });

        service = micro(pdfRendererHandler);

        Promise.all([
            new Promise(resolve =>
                staticServer.listen(staticPort, () => {
                    resolve(`http://localhost:${staticPort}`);
                })
            ),
            listen(service),
        ]).then(([, servUrl]) => {
            serviceUrl = servUrl;
            done();
        });
    });

    after(done => {
        let runningServerCount = 2;
        const closeHandler = () => {
            runningServerCount--;
            if (runningServerCount === 0) {
                done();
            }
        };
        service.close(closeHandler);
        staticServer.close(closeHandler);
    });

    describe('Распечатка', function() {
        it('Базовый вариант', async function() {
            const response = await getPdfResponse('basic');
            assert.equal(response.statusCode, 200);
            assert.ok(response.body);
        });

        it('В вертикальной проекции', async function() {
            this.timeout(20000);

            const response = await getPdfResponse('basic_tutor', {
                time: true,
            });
            assert.equal(response.statusCode, 200);
            assert.ok(response.body);
        });

        it('Книжный вариант', async function() {
            const response = await getPdfResponse('nup');
            assert.equal(response.statusCode, 200);
            assert.ok(response.body);
        });

        it('Большой документ', async function() {
            this.timeout(20000);
            const response = await getPdfResponse('big', {
                time: true,
            });
            assert.equal(response.statusCode, 200);
            assert.ok(response.body);
        });
    });

    describe('Невалидные входные данные', function() {
        it('Не POST запрос', async function() {
            try {
                await request({
                    uri: serviceUrl,
                    method: 'GET',
                    resolveWithFullResponse: true,
                });
            } catch (err) {
                assert.equal(err.statusCode, 400);
            }
        });

        it('Не отправлен url', async function() {
            try {
                await getPdfResponse('without_url');
            } catch (err) {
                assert.equal(err.statusCode, 400);
            }
        });

        it('Невалидный хост', async function() {
            const response = await getPdfResponse('error_url');
            assert.ok(!response.body);
        });

        it('Не http/https url', async function() {
            try {
                await getPdfResponse('invalid_url');
            } catch (err) {
                assert.equal(err.statusCode, 400);
            }
        });

        it('Нет тела запроса', async function() {
            try {
                await getPdfResponse('empty_request');
            } catch (err) {
                assert.equal(err.statusCode, 400);
            }
        });
    });
});
