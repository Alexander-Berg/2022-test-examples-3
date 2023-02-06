const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-edit-state', () => {
    const AskAbcServiceEditState = require('./ask-abc-service-edit-state');

    it('Should send body.state and body.force to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.patch('/api/frontend/services/:serviceId/', (req, res) => {
                    assert.strictEqual(req.params.serviceId, '123');
                    assert.strictEqual(req.body.state, 'state');
                    assert.strictEqual(req.body.force, 'force');
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

                app.post('/services/:serviceId', AskAbcServiceEditState.create({
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
                        state: 'state',
                        force: 'force',
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
