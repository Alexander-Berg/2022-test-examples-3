const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-move', () => {
    const AskAbcServiceMove = require('./ask-abc-service-move');

    it('Should send body.service && body.destination to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.post('/api/frontend/services/move-requests/', (req, res) => {
                    assert.strictEqual(req.body.service, '123');
                    assert.strictEqual(req.body.destination, 'destination');
                    res.json({ error: 'nope' });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.post('/services/:serviceId/', AskAbcServiceMove.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.service, { error: 'nope' });
                    res.sendStatus(555);
                });

                supertest(app)
                    .post('/services/123/')
                    .send({
                        destination: 'destination',
                    })
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });
});
