const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

const logger = {
    child() {
        return {
            info() {},
            debug() {},
        };
    },
};

describe('express/middlewares/ask-abc-service-edit-attr', () => {
    const AskAbcServiceEditAttr = require('./ask-abc-service-edit-attr');

    it('Should provide POST method', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.post('/test', (req, res) => {
                    assert.strictEqual(req.method, 'POST');
                    res.json({});
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    next();
                });

                app.post('/', AskAbcServiceEditAttr.create({
                    port,
                    pathname: '/test',
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .post('/')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should correctly format ans', done => {
        fakeBack(
            back => {
                back.use(jsonParser());

                back.post('/test', (req, res) => {
                    assert.strictEqual(req.method, 'POST');
                    res.json({
                        error: 'nope',
                    });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());
                app.use(jsonParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    next();
                });

                class TestAskAbcServiceEditAttr extends AskAbcServiceEditAttr {
                    formatRes(ans) {
                        const result = super.formatRes(ans);

                        assert.strictEqual(result, Object(result));
                        assert.strictEqual(result.error, 'nope');

                        return result;
                    }
                }

                app.post('/', TestAskAbcServiceEditAttr.create({
                    port,
                    pathname: '/test',
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.service, { error: 'nope' });
                    res.sendStatus(555);
                });

                supertest(app)
                    .post('/')
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
