const path = require('path');
const express = require('express');
const supertest = require('supertest');

describe('express/middlewares/abc-controller-html-bemhtml', () => {
    const AbcControllerHtmlBemhtml = require('./abc-controller-html-bemhtml');

    class AbcControllerHtmlBemhtmlTest extends AbcControllerHtmlBemhtml {
        getBundlePathTmpl() {
            return path.join(__dirname, '../../../test/fixtures/bundles/%s/%s.%s.bemhtml.node.js');
        }

        getInlinePath() {
            return path.join(__dirname, '../../../test/fixtures/bundles/inline/inline.js');
        }
    }

    it('Should respond with html', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.lang = 'ru';
            res.locals.configs = { languages: ['ru', 'en'] };
            res.locals.permissions = [];
            res.locals.dispenserPermissions = [];
            next();
        });

        app.get('/', AbcControllerHtmlBemhtmlTest.create({
            type: 'test',
        }));

        supertest(app)
            .get('/')
            .expect('Content-Type', /^text\/html/)
            .expect(/<test-ru>{"block":"b-page","mods":{"type":"test"},"type":"test","data":{"lang":"ru","configs":{"languages":.*<\/test-ru>/)
            .expect(200)
            .end(done);
    });

    it('Should respond with json on ?__mode=json', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals.lang = 'ru';
            res.locals.configs = { languages: ['ru', 'en'] };
            next();
        });

        app.use(AbcControllerHtmlBemhtmlTest.create({
            type: 'test',
            debug: true,
        }));

        supertest(app)
            .get('/?__mode=json')
            .expect('Content-Type', /^application\/json/)
            .expect(/{"lang":"ru","configs":{"languages":.*}/)
            .expect(200)
            .end(done);
    });
});
