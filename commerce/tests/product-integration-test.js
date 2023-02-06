'use strict';

const nock = require('nock');
const request = require('supertest');
const { expect } = require('chai');
const { Validator } = require('jsonschema');
const middlewareMock = require('./helper/middleware-mock');
const nockTvm = require('./helper/tvm');

describe('Product integration', () => {
    let app;
    const v = new Validator();

    function getSchema(productType) {
        return {
            type: 'object',
            properties: {
                product: require(`./.schemas/${productType}`),
                media: require('./.schemas/media'),
                sections: require('./.schemas/page-sections')
            },
            required: ['product', 'media', 'sections']
        };
    }

    before(nockTvm);

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    after(nock.cleanAll);

    afterEach(middlewareMock.integrationAfter);

    describe('`/products/display`', () => {
        it('should return correct data on /adv/products/display', done => {
            const schema = getSchema('product');

            request(app)
                .get('/adv/products/display')
                .set('host', 'yandex.ru')
                .expect(200)
                .end((err, data) => {
                    const actual = data.body;

                    expect(actual.currentPath).to.be.equal('/products/display');
                    expect(actual.section).to.be.equal('display');
                    expect(actual.isRoot).to.be.true;
                    expect(v.validate(data.body, schema).valid).to.be.true;

                    done(err);
                });
        });

        it('should return correct data on /adv/products/display/mainpage', done => {
            request(app)
                .get('/adv/products/display/mainpage')
                .set('host', 'yandex.ru')
                .expect(200)
                .end((err, data) => {
                    const actual = data.body;

                    expect(actual.currentPath).to.be.equal('/products/display/mainpage');
                    expect(actual.section).to.be.equal('display');
                    expect(actual.isRoot).to.be.false;

                    done(err);
                });
        });

        it('should return 404 on /adv/display', done => {
            request(app)
                .get('/adv/display')
                .set('host', 'yandex.ru')
                .expect(404, done);
        });
    });

    it('should return 200 on /adv/products', done => {
        const schema = getSchema('product');

        request(app)
            .get('/adv/products')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                const actual = data.body;

                expect(actual.currentPath).to.be.equal('/products');
                expect(actual.section).to.be.equal('products');
                expect(actual.isRoot).to.be.true;
                expect(v.validate(data.body, schema).valid).to.be.true;

                done(err);
            });
    });
});
