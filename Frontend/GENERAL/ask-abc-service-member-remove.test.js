const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-member-remove', () => {
    const AskAbcServiceMemberRemove = require('./ask-abc-service-member-remove');

    it('Should send memberId to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.delete('/api/frontend/services/members/:memberId/', (req, res) => {
                    assert.strictEqual(req.params.memberId, 'member');
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

                app.post('/services/:serviceId', AskAbcServiceMemberRemove.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepStrictEqual(res.locals.service, {}); // мидлварина игнорирует ответ ручки DELETE
                    res.sendStatus(555);
                });

                supertest(app)
                    .post('/services/123')
                    .send({
                        member: 'member',
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
