const assert = require('assert');

const supertest = require('supertest');
const express = require('express');
const cookieParser = require('cookie-parser');
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-readonly', () => {
    const AskAbcReadonly = require('./ask-abc-readonly');

    it('Should set status.readonly', done => {
        const expect = 'foo';

        fakeBack(
            back => {
                back.get('/common/readonly/', (req, res) => {
                    res.status(200).json({ content: { status: expect } });
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

                app.use(AskAbcReadonly.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.status.readonly, expect);
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
