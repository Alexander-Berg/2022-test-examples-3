const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-department-decline', () => {
    const AskAbcServiceDepartmentDecline = require('./ask-abc-service-department-decline');

    it('Should send body.idm_role to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.post('/api/frontend/services/departments/decline/', (req, res) => {
                    assert.strictEqual(req.body.idm_role, 'department');
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

                app.post('/services/:serviceId', AskAbcServiceDepartmentDecline.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.service, {});
                    res.sendStatus(555);
                });

                supertest(app)
                    .post('/services/123')
                    .send({
                        department: 'department',
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
