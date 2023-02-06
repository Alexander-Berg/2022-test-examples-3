'use strict';

const nock = require('nock');
const request = require('supertest');
const { expect } = require('chai');
const { Validator } = require('jsonschema');
const middlewareMock = require('./helper/middleware-mock');
const nockTvm = require('./helper/tvm');

describe('PageModel integration', () => {
    let app;
    const v = new Validator();
    const schema = {
        type: 'object',
        properties: {
            product: require('./.schemas/page'),
            media: require('./.schemas/media'),
            sections: require('./.schemas/page-sections')
        },
        required: ['product', 'media', 'sections']
    };

    before(nockTvm);

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    after(nock.cleanAll);

    afterEach(middlewareMock.integrationAfter);

    describe('`/actions/alfabank`', () => {
        it('should return correct data on /adv/actions/alfabank', done => {
            request(app)
                .get('/adv/actions/alfabank')
                .set('host', 'yandex.ru')
                .expect(200)
                .end((err, data) => {
                    const actual = data.body;

                    expect(actual.currentPath).to.be.equal('/actions/alfabank');
                    expect(actual.section).to.be.equal('actions');
                    expect(actual.isRoot).to.be.false;
                    expect(v.validate(data.body, schema).valid).to.be.true;

                    done(err);
                });
        });

        it('should return 301 on /adv/products/materials', done => {
            request(app)
                .get('/adv/products/materials')
                .set('host', 'yandex.ru')
                .expect(301, done);
        });
    });

    it('should return 404 on /materials2', done => {
        request(app)
            .get('/adv/materials2')
            .set('host', 'yandex.ru')
            .expect(404, done);
    });
});
