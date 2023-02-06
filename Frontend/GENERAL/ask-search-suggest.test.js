const assert = require('assert');

const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-search-suggest', () => {
    const AskAbcSearchSuggest = require('./ask-search-suggest');

    it('Should send params to backend', done => {
        fakeBack(
            back => {
                const expect = { result: [{}] };

                back.get('/suggest/', (req, res) => {
                    const query = req.query;

                    assert.strictEqual(query.version, '2');
                    assert.strictEqual(query.layers, 'people');
                    assert.strictEqual(query.text, 'test');
                    res.json(expect);
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

                app.use(AskAbcSearchSuggest.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/suggest/people/?version=2&layers=people&text=test')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should not fail on 400 status', done => {
        const expect = { result: [] };

        fakeBack(
            back => {
                back.get('/suggest/', (req, res) => {
                    const query = req.query;

                    assert.strictEqual(query.version, '2');
                    assert.strictEqual(query.layers, 'people');
                    assert.strictEqual(query.text, 'test');

                    res.status(400);
                    res.json(expect);
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

                app.use(AskAbcSearchSuggest.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.searchSuggest, expect);
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/suggest/people/?version=2&layers=people&text=test')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should correctly format staff data', done => {
        const expect = {
            result: [{
                department_name: 'dep',
                id: 777,
                is_dismissed: false,
                is_memorial: false,
                is_robot: false,
                layer: 'people',
                login: 'tlogin',
                name: { first: 'tname', last: 'tlast', middle: 'tmiddle' },
                position: 'pos',
                staff_id: '123',
                title: 'tname tlast',
                uid: '12345',
                url: '//url',
                click_urls: ['//clck'],
            }],
        };

        fakeBack(
            back => {
                back.get('/suggest/', (req, res) => {
                    const query = req.query;

                    assert.strictEqual(query.version, '2');
                    assert.strictEqual(query.layers, 'people');
                    assert.strictEqual(query.text, 'test');

                    res.json(expect);
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

                app.use(AskAbcSearchSuggest.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.searchSuggest, expect);
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/?version=2&layers=people&text=test')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should send support several layers values in params', done => {
        fakeBack(
            back => {
                const expect = { result: [{}] };

                back.get('/suggest/', (req, res) => {
                    const query = req.query;
                    assert.strictEqual(query.version, '2');
                    assert.deepEqual(query.layers, ['people', 'groups']);
                    assert.strictEqual(query.text, 'test');
                    res.json(expect);
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

                app.use(AskAbcSearchSuggest.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/suggest/people/?version=2&layers=people&layers=groups&text=test')
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
