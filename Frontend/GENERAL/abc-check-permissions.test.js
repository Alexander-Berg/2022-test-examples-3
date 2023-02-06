const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

const fakeLogger = (req, res, next) => {
    req.logger = {
        child() {
            return {
                info() {},
                debug() {},
            };
        },
    };
    next();
};

describe('express/middlewares/ask-check-permissions', () => {
    const AskAbcPermissions = require('./ask-abc-permissions');
    const AbcCheckPermissions = require('./abc-check-permissions');

    it('Should throw 403 error without permissions', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/api/frontend/permissions/', (req, res) => {
                    res.json({ results: new Array() });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());
                app.use(fakeLogger);

                app.use(AskAbcPermissions.create({
                    port,
                    hostname,
                }));

                app.use(AbcCheckPermissions.create());

                // eslint-disable-next-line no-unused-vars,@typescript-eslint/no-unused-vars
                app.use((err, req, res, next) => {
                    assert.strictEqual(err.message, 'Access denied');
                    assert.strictEqual(err.status, '403');

                    res.sendStatus(555);
                });

                app.get('/', (req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });
});
