'use strict';

const request = require('supertest');
const { expect } = require('chai');
const { Validator } = require('jsonschema');
const middlewareMock = require('./helper/middleware-mock');

describe('Product integration', () => {
    let app;
    const v = new Validator();
    const schema = {
        type: 'object',
        properties: {
            product: require('./.schemas/product'),
            media: require('./.schemas/media'),
            sections: require('./.schemas/page-sections')
        },
        required: ['product', 'media', 'sections']
    };

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    describe('`/products/display`', () => {
        it('should return correct data on /adv/products/display/', done => {
            request(app)
                .get('/adv/products/display/')
                .set('host', 'yandex.ru')
                .expect(200)
                .end((err, data) => {
                    const actual = data.body;

                    expect(actual.currentPath).to.be.equal('/adv/products/display/');
                    expect(actual.section).to.be.equal('display');
                    expect(actual.isRoot).to.be.true;
                    expect(v.validate(data.body, schema).valid).to.be.true;

                    done(err);
                });
        });

        it('should return correct data on /adv/products/display/mainpage/', done => {
            request(app)
                .get('/adv/products/display/mainpage/')
                .set('host', 'yandex.ru')
                .expect(200)
                .end((err, data) => {
                    const actual = data.body;

                    expect(actual.currentPath).to.be.equal('/adv/products/display/mainpage/');
                    expect(actual.section).to.be.equal('display');
                    expect(actual.isRoot).to.be.false;
                    expect(v.validate(data.body, schema).valid).to.be.true;

                    done(err);
                });
        });

        it('should return 404 on /adv/display/', done => {
            request(app)
                .get('/adv/display/')
                .set('host', 'yandex.ru')
                .expect(404, done);
        });
    });

    it('should return 404 on /adv/products/', done => {
        request(app)
            .get('/adv/products/')
            .set('host', 'yandex.ru')
            .expect(404, done);
    });

    afterEach(middlewareMock.integrationAfter);
});
