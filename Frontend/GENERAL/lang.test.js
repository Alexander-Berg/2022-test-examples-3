const assert = require('assert');

const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/lang', () => {
    const Lang = require('./lang');

    it('Should add res.locals.lang', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                sessionid: { lang: 'qqq' },
            };
            next();
        });

        app.use(Lang.create({
            languages: ['aaa', 'qqq'],
        }));

        app.get('/', (req, res) => {
            assert.strictEqual(res.locals.lang, 'qqq');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should add req.lang', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                sessionid: { lang: 'qqq' },
            };
            next();
        });

        app.use(Lang.create({
            languages: ['aaa', 'qqq'],
        }));

        app.get('/', (req, res) => {
            assert.strictEqual(req.lang, 'qqq');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should take first lang from params if server does not support current from sessionid', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                sessionid: { lang: 'rrr' },
            };
            next();
        });

        app.use(Lang.create({
            languages: ['aaa', 'qqq'],
        }));

        app.get('/', (req, res) => {
            assert.strictEqual(res.locals.lang, 'aaa');
            assert.strictEqual(req.lang, 'aaa');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });

    it('Should take lang from query if server support', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                sessionid: { lang: 'aaa' },
            };
            next();
        });

        app.use(Lang.create({
            languages: ['aaa', 'qqq'],
        }));

        app.get('/', (req, res) => {
            assert.strictEqual(res.locals.lang, 'qqq');
            assert.strictEqual(req.lang, 'qqq');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/?lang=qqq')
            .expect(555)
            .end(done);
    });

    it('Should take lang from sessionid if server not support lang from query', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                sessionid: { lang: 'qqq' },
            };
            next();
        });

        app.use(Lang.create({
            languages: ['aaa', 'qqq'],
        }));

        app.get('/', (req, res) => {
            assert.strictEqual(res.locals.lang, 'qqq');
            assert.strictEqual(req.lang, 'qqq');
            res.sendStatus(555);
        });

        supertest(app)
            .get('/?lang=sss')
            .expect(555)
            .end(done);
    });
});
