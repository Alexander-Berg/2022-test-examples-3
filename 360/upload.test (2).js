'use strict';

const router = require('./upload.js');
const got = require('got');
const nock = require('nock');
const express = require('@yandex-int/duffman').express;
const authMock = require('../../test/mock/auth.json');
const FormData = require('form-data');
const stream = require('stream');
const streamToArray = require('stream-to-array');
const _merge = require('lodash/merge');
const Core = require('../../routes/helpers/extra-core.js');

let core;
let server;
let request;
let akitaNock;

beforeEach((done) => {
    const routePath = '/api/mobile/v1/upload';
    const app = express();

    app.set('port', 0);
    app.set('x-powered-by', false);
    app.use(routePath, router);

    server = app.listen(done);
    const port = server.address().port;

    const req = {
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/apimobile/v1',
            'x-request-id': '12345',
            'x-real-ip': '2a02:6b8::25'
        },
        query: {
            uuid: 'deadbeef42',
            client: 'iphone',
            client_version: '10.0.3'
        },
        body: {}
    };
    const res = {
        set: jest.fn(),
        on: jest.fn(),
        send: jest.fn(),
        status: jest.fn()
    };
    core = new Core(req, res);

    request = (options) => {
        options = options || {};
        options.followRedirect = false;
        options.agent = false;
        options.method = 'POST';

        options.query = _merge(options.query || {}, {
            uuid: '0123456789',
            client: 'iphone',
            client_version: '10.0.3'
        });
        options.headers = _merge(options.headers || {}, {
            'x-real-ip': 'dead::beef',
            'x-request-id': 'ababbabababba',
            'x-original-host': 'test',
            'x-https-request': 'yes',
            'user-agent': 'aser ugent',
            'x-api-method': 'upload'
        });

        return got(`http://localhost:${port}${routePath}`, options);
    };

    akitaNock = nock(core.config.services.akita).filteringPath((path) => path.replace(/\?.*/, ''));
});

afterEach((done) => {
    server.close(done);
    nock.cleanAll();
});

test('-> PERM_FAIL когда нет файла', async () => {
    const res = await request({ json: true });

    expect(res.statusCode).toBe(200);
    expect(res.body.status.status).toBe(2);
    expect(res.body.status.phrase).toInclude('no file');
});

test('-> PERM_FAIL когда файл слишком большой', async () => {
    akitaNock.get('/auth').reply(200, authMock);

    const options = await generateFileBody({
        buffer: Buffer.alloc(30 * 1024),
        mimetype: 'test',
        originalname: 'test'
    });

    const resp = await request(options);

    const status = JSON.parse(resp.body).status;
    expect(resp.statusCode).toBe(200);
    expect(status.status).toBe(3);
    expect(status.phrase).toInclude('File too large');
});

test('должен вернуть PERM_FAIL(3), если нет авторизации', async () => {
    akitaNock.get('/auth').reply(200, { error: { code: '2001' } });

    const headers = { authorization: 'Oauth 00000' };

    const options = await generateFileBody({
        buffer: Buffer.alloc(1024),
        mimetype: 'test',
        originalname: 'test'
    });

    const resp = await request(_merge(options, { headers }));

    const body = JSON.parse(resp.body);
    expect(resp.statusCode).toBe(200);
    expect(body.status.status).toBe(3);
    expect(body.status.phrase).toInclude('2001');
});

async function generateFileBody(file) {
    const form = new FormData();
    form.append('attachment', file.buffer, { contentType: file.mimetype, filename: file.originalname });
    const formStream = new stream.PassThrough();
    form.pipe(formStream);

    const parts = await streamToArray(formStream);

    const buffers = parts.map((part) => Buffer.isBuffer(part) ? part : Buffer.from(part));
    const buffer = Buffer.concat(buffers);

    return {
        body: buffer,
        headers: form.getHeaders()
    };
}
