const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const RequestId = require('tools-access-express/express/middlewares/request-id');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

const logger = {
    child() {
        return {
            error() {},
            info() {},
            debug() {},
        };
    },
};

describe('express/middlewares/ask-abc', () => {
    const AskAbc = require('./ask-abc');

    it('Should send required default params to backend', done => {
        fakeBack(
            back => {
                back.use(cookieParser());
                back.use((req, res) => {
                    assert.strictEqual(req.method, 'GET');
                    assert.ok(req.get('referer'));
                    assert.strictEqual(req.get('X-Forwarded-Request-Id'), '100500');
                    assert.strictEqual(req.get('accept-language'), 'lang');
                    assert.strictEqual(req.cookies.Session_id, 'Sessid');
                    res.status(200).json({});
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(RequestId.create());
                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    res.locals.lang = 'lang';
                    next();
                });

                app.use(AskAbc.create({
                    port,
                    hostname,
                    pathname: '/',
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/')
                    .set('x-request-id', '100500')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should add referer header', done => {
        const app = express();

        app.use(cookieParser());

        app.use((req, res, next) => {
            req.logger = logger;
            next();
        });

        class TestAskAbc extends AskAbc {
            main() {
                assert.strictEqual(this.params.headers.referer, 'baz:foo.bar');
                this.res.status(555).json({});
            }

            _assertStatusOk() {}
        }

        app.use(TestAskAbc.create({
            protocol: 'baz',
            hostname: 'foo.bar',
            pathname: '/',
        }));

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should send json to backend', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.use((req, res) => {
                    assert.deepEqual(req.body, { foo: 42 });
                    res.status(201).json({});
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    next();
                });

                class TestAskAbc extends AskAbc {
                    createParams(params) {
                        return this.extendParams(super.createParams(params), {
                            body: { foo: 42 },
                        });
                    }

                    _assertStatusOk(ans) {
                        assert.strictEqual(ans.statusCode, 201);
                        this.set('done', true);
                    }
                }

                app.use(TestAskAbc.create({
                    port,
                    hostname,
                    pathname: '/',
                }));

                app.use((req, res) => {
                    assert.ok(res.locals.done);
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

    it('Should not accept != 200 responses', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.use((req, res) => {
                    res.status(400).json({ error: { message: 'asdas' } });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    next();
                });

                class TestAskAbc extends AskAbc {
                    main() {
                        return super.main().catch(err => {
                            assert.strictEqual(err.status, 400);
                            assert.deepEqual(err.data, { message: 'asdas' });
                            this.set('done', true);
                        });
                    }
                }

                app.use(TestAskAbc.create({
                    port,
                    hostname,
                    pathname: '/',
                }));

                app.use((req, res) => {
                    assert.ok(res.locals.done);
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

    it('Should support not a json responses', done => {
        fakeBack(
            back => {
                back.use(jsonParser());
                back.use((req, res) => {
                    res.status(400).send('asdasd');
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    next();
                });

                class TestAskAbc extends AskAbc {
                    main() {
                        return super.main().catch(err => {
                            assert.strictEqual(err.status, 400);
                            assert.deepEqual(err.data, { message: 'asdasd' });
                            this.set('done', true);
                        });
                    }
                }

                app.use(TestAskAbc.create({
                    port,
                    hostname,
                    pathname: '/',
                }));

                app.use((req, res) => {
                    assert.ok(res.locals.done);
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

    it('Should not continue if got error', done => {
        const app = express();

        app.use(cookieParser());

        app.use((req, res, next) => {
            req.logger = logger;
            next();
        });

        class TestAskAbc extends AskAbc {
            main() {
                return super.main().catch(err => {
                    assert.ok(err);
                    this.set('done', true);
                });
            }

            createParams(params) {
                return this.extendParams(super.createParams(params), {
                    timeout: 0,
                });
            }
        }

        app.use(TestAskAbc.create({ hostname: 'blah' }));

        app.use((req, res) => {
            assert.ok(res.locals.done);
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should expose data.content to res.locals.<name>', done => {
        fakeBack(
            back => {
                back.use((req, res) => {
                    res.status(201).json({ content: { foo: 'bar' } });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    next();
                });

                class TestAskAbc extends AskAbc {
                    get name() {
                        return 'path';
                    }

                    _assertStatusOk() {}
                }

                app.use(TestAskAbc.create({
                    port,
                    hostname,
                    pathname: '/',
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.path, { foo: 'bar' });
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

    it('Should add data.content to res.locals.<name>', done => {
        fakeBack(
            back => {
                back.use((req, res) => {
                    res.status(201).json({ content: { d: 43 } });
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = logger;
                    res.locals = { a: { b: { c: 42 } } };
                    next();
                });

                class TestAskAbc extends AskAbc {
                    get name() {
                        return 'a.b';
                    }

                    _assertStatusOk() {}
                }

                app.use(TestAskAbc.create({
                    port,
                    hostname,
                    pathname: '/',
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals, { a: { b: { c: 42, d: 43 } } });
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
