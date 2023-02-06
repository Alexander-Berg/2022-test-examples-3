'use strict';

const got = require('got');
const express = require('express');
const yandexuid = require('./yandexuid.js');

let server;
let request;

beforeEach((done) => {
    const app = express();
    app.set('port', 0);
    // эмулируем cookie-parser и express-uatraits
    app.use((req, res, next) => {
        req.cookies = {};
        req.uatraits = {
            isBrowser: true,
            SameSiteSupport: true
        };
        next();
    });
    app.use(yandexuid);
    app.get('/', function(req, res) { res.end(); });

    server = app.listen(done);
    const port = server.address().port;

    request = (options = {}) => {
        options.agent = false;
        return got(`http://127.0.0.1:${port}/`, options);
    };
});

afterEach((done) => {
    server.close(done);
});

test('ставит куку', async () => {
    const res = await request({
        headers: {
            host: 'mail.yandex.ru'
        }
    });

    expect(res.headers['set-cookie'][0]).toMatch('Secure; SameSite=None');
    expect(res.headers['set-cookie'][0]).toMatch('Domain=.yandex.ru');
});

test('ставит куку на yandex-team.ru', async () => {
    const res = await request({
        headers: {
            host: 'mail.yandex-team.ru'
        }
    });

    expect(res.headers['set-cookie'][0]).toMatch('Secure; SameSite=None');
    expect(res.headers['set-cookie'][0]).toMatch('Domain=.yandex-team.ru');
});

test('учитывает x-original-host', async () => {
    const res = await request({
        headers: {
            'x-original-host': 'mail.yandex.com'
        }
    });

    expect(res.headers['set-cookie'][0]).toMatch('Secure; SameSite=None');
    expect(res.headers['set-cookie'][0]).toMatch('Domain=.yandex.com');
});
