'use strict';

const _ = require('lodash');
const { expect } = require('chai');

const ProductModel = require('../../server/model/product');

describe('Product model', () => {
    const bunker = require('../mock/bunker');
    const pagesRelations = _.chain(bunker).get('settings.ru.relations')
        .keyBy('slug')
        .value();
    const product = new ProductModel({
        bunker, pagesRelations
    }, {
        section: 'display',
        page: 'mainpage'
    });

    beforeEach(() => {
        product.sectionInfo.isProduct = true;
    });

    it('should return correct `menuRoot`', () => {
        expect(product.menuRoot).to.be.equal('/adv/products/display');
    });

    it('should return page data', done => {
        product
            .getData()
            .then(data => {
                expect(data.enabled).to.be.true;
                expect(data).to.contain.all.keys(['media', 'product', 'sections']);

                done();
            })
            .catch(err => {
                done(err);
            });
    });

    it('should return 404 when `!isProduct`', done => {
        product.sectionInfo.isProduct = false;

        product
            .getData()
            .catch(err => {
                expect(err).to.deep.equal({
                    internalCode: '404_PNF',
                    message: 'Page /display/mainpage was not found'
                });

                done();
            });
    });
});
