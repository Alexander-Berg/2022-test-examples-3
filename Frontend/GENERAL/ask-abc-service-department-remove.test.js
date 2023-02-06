const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-department-remove', () => {
    const AskAbcServiceDepartmentRemove = require('./ask-abc-service-department-remove');

    it('Should send departmentId to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.delete('/api/frontend/services/departments/:departmentId/', (req, res) => {
                    assert.strictEqual(req.params.departmentId, 'department');
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

                app.post('/services/:serviceId', AskAbcServiceDepartmentRemove.create({
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
