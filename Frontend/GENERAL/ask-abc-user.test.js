const assert = require('assert');

const supertest = require('supertest');
const express = require('express');
const cookieParser = require('cookie-parser');
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-user', () => {
    const AskAbcUser = require('./ask-abc-user');

    it('Should get user info from backend', done => {
        fakeBack(
            back => {
                back.get('/api/frontend/common/user/', (req, res) => {
                    res.status(200).json({
                        login: 'spacy',
                    });
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

                app.use(AskAbcUser.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    let result = res.locals.user;

                    assert.deepEqual(result, { login: 'spacy' });

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
