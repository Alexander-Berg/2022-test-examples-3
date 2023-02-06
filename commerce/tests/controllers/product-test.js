'use strict';

const { expect } = require('chai');

const Product = require('../../server/controllers/product');

describe('Product controller', () => {
    const product = new Product({
        params: {
            section: 'display',
            page: 'mainpage'
        }
    });

    it('should return correct `seoSection`', () => {
        expect(product.seoSection).to.be.equal('products');
    });

    it('should return Model as ProductModel', () => {
        const instance = new product.Model({}, {});

        expect(instance.constructor.name).to.be.equal('Product');
    });
});
