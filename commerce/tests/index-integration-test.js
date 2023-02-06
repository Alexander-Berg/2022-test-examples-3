const nock = require('nock');
const request = require('supertest');
const middlewareMock = require('./helper/middleware-mock');
const nockTvm = require('./helper/tvm');

describe('Index page', () => {
    let app;

    before(nockTvm);

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    after(nock.cleanAll);

    afterEach(middlewareMock.integrationAfter);

    it('respond with OK', done => {
        request(app)
            .get('/adv/ping')
            .set('host', 'yandex.ru')
            .expect(200, done);
    });

    it('static works', done => {
        request(app)
            .get('/vda/static/desktop.bundles/index/_index.css')
            .expect('Cache-Control', /max-age=31536000/) // 365 days in secs
            .expect(200, done);
    });
});
