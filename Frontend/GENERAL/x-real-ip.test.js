const assert = require('assert');
const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/x-real-ip', () => {
    const XRealIp = require('./x-real-ip');

    it('Should trust X-Real-Ip header as client IP', done => {
        const app = express();

        app.enable('trust proxy');
        app.use(XRealIp.create());

        app.get('/', (req, res) => {
            assert.strictEqual(req.ip, '2a02:6b8::3cb');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .set({ 'X-Forwarded-For': '2a02:6b8:0:3400::aaaa' })
            .set({ 'X-Real-Ip': '2a02:6b8::3cb' })
            .expect(555)
            .end(done);
    });
});
