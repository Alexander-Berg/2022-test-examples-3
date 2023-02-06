'use strict';

const nock = require('nock');
const request = require('supertest');
const { expect } = require('chai');
const { Validator } = require('jsonschema');
const middlewareMock = require('./helper/middleware-mock');
const nockTvm = require('./helper/tvm');

describe('Prices page', () => {
    let app;

    const v = new Validator();

    const Product = {
        type: 'object',
        properties: {
            id: { type: 'string' },
            url: { type: 'string' },
            title: { type: 'string' },
            tables: { type: 'array' },
            prices: { type: 'array' }
        }
    };

    const Price = {
        type: 'object',
        properties: {
            title: { type: 'string' },
            tableName: { type: 'string' },
            id: { type: 'string' },
            header: {
                type: 'array',
                minItems: 2
            },
            rows: {
                type: 'array'
            },
            information: { type: 'array' },
            priceUrl: { type: 'string' }
        }
    };

    before(nockTvm);

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    after(nock.cleanAll);

    afterEach(middlewareMock.integrationAfter);

    it('should return prices page', done => {
        request(app)
            .get('/adv/prices')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                const { products } = data.res.body;
                const [firstBlock] = products.data;

                expect(data.res.body.page).to.equal('prices');
                expect(data.res.body.type).to.equal('list');
                expect(firstBlock.list).to.be.an('array');

                firstBlock.list.forEach(product => {
                    expect(v.validate(product, Product).valid).to.equal(true);

                    product.prices.filter(Boolean).forEach(price => {
                        expect(v.validate(price, Price).valid).to.equal(true);
                    });
                });

                done(err);
            });
    });

    it('should return tables page', done => {
        request(app)
            .get('/adv/prices-common')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                const { products } = data.res.body;
                const [firstBlock] = products.data;

                expect(data.res.body.page).to.equal('prices');
                expect(data.res.body.type).to.equal('common');
                expect(firstBlock.list).to.be.an('array');

                firstBlock.list.forEach(product => {
                    expect(v.validate(product, Product).valid).to.equal(true);
                    expect(product.prices).to.have.lengthOf(0);

                    product.tables.forEach(table => {
                        expect(v.validate(table, Price).valid).to.equal(true);
                    });
                });

                done(err);
            });
    });
});
