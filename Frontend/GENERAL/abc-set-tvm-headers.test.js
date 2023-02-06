const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');

const AbcSetTvmHeaders = require('./abc-set-tvm-headers');

const ABC_TVM_TICKET = 'abc-tvm-header';
const D_TVM_TICKET = 'd-tvm-header';
const BB_USER_TICKET = 'bb-user-header';

describe('express/middlewares/abc-set-tvm-headers', () => {
    const app = express();

    app.use(cookieParser());
    app.use((req, _, next) => {
        req.tvm = {
            tickets: {
                abc_api: {
                    ticket: ABC_TVM_TICKET,
                },
                dispenser: {
                    ticket: D_TVM_TICKET,
                },
            },
        };

        req.blackbox = {
            raw: {
                user_ticket: BB_USER_TICKET,
            },
        };

        next();
    });

    it('Should set tvm headers default for abc_api destination', done => {
        app.use(AbcSetTvmHeaders.create());

        app.use((req, res) => {
            expect(req.headers['x-ya-service-ticket']).toBe(ABC_TVM_TICKET);
            expect(req.headers['x-ya-user-ticket']).toBe(BB_USER_TICKET);
            res.sendStatus(555);
        });

        app.get('/', (req, res) => {
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end((...args) => {
                done(...args);
            });
    });

    it('Should set tvm headers default for dispenser destination', done => {
        app.use(AbcSetTvmHeaders.create({ dst: 'dispenser' }));

        app.use((req, res) => {
            expect(req.headers['x-ya-service-ticket']).toBe(D_TVM_TICKET);
            expect(req.headers['x-ya-user-ticket']).toBe(BB_USER_TICKET);

            res.sendStatus(555);
        });

        app.get('/', (req, res) => {
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end((...args) => {
                done(...args);
            });
    });

    it('Should not set headers for unknown destinations or without tickets', done => {
        app.use(AbcSetTvmHeaders.create({ dst: 'unknown' }));

        app.use((req, res) => {
            expect(req.headers['x-ya-service-ticket']).toBeUndefined();
            expect(req.headers['x-ya-user-ticket']).toBeUndefined();

            res.sendStatus(555);
        });

        app.get('/', (req, res) => {
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end((...args) => {
                done(...args);
            });
    });
});
