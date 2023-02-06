const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-service-scopes', () => {
    const AskAbcServiceScopes = require('./ask-abc-service-scopes');

    it('Should get service scopes from backend', done => {
        const expect = 'foo';

        fakeBack(
            back => {
                back.get('/api/v4/roles/scopes/', (req, res) => {
                    res.json({ results: expect });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

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

                app.use(AskAbcServiceScopes.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.service.scopes, expect);
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/')
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
