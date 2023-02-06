const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-edit-name', () => {
    const AskAbcServiceEditName = require('./ask-abc-service-edit-name');

    it('Should send body.name to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.patch('/api/frontend/services/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
                    assert.strictEqual(req.body.name, 'name');
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

                app.post('/services/:serviceId', AskAbcServiceEditName.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.service, { error: 'nope' });
                    res.sendStatus(555);
                });

                supertest(app)
                    .post('/services/123')
                    .send({
                        name: 'name',
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
