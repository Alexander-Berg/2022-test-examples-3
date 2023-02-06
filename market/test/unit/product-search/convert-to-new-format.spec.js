'use strict';

const convertToNewFormat = require('../../../middleware/prepare-query/convert-product-info-to-new-format');

describe('convert to new format', () => {
    const defaultQuery = {
        text: 'name',
        price: 'price',
        currency: 'currency',
        url: 'url',
        unknownField: 'unknownField'
    };

    test('should convert fields extracted by selectors', () => {
        const query = Object.assign({}, defaultQuery, { method: 'selectors' });

        const expectedResult = Object.assign({}, query, {
            name_by_selectors: 'name',
            price_by_price: 'price',
            currency_by_selectors: 'currency',
            url_by_selectors: 'url'
        });

        convertToNewFormat({ query }, null, () => {});

        expect(query).toEqual(expectedResult);
    });

    test('should convert fields extracted by microdata', () => {
        const query = Object.assign({}, defaultQuery, { method: 'microdata' });

        const expectedResult = Object.assign({}, query, {
            name_by_microdata: 'name',
            price_by_price: 'price',
            currency_by_microdata: 'currency',
            url_by_microdata: 'url'
        });

        convertToNewFormat({ query }, null, () => {});

        expect(query).toEqual(expectedResult);
    });

    test('should convert fields extracted by cms', () => {
        const query = Object.assign({}, defaultQuery, { method: 'cms' });

        const expectedResult = Object.assign({}, query, {
            name_by_cms: 'name',
            price_by_price: 'price',
            currency_by_cms: 'currency',
            url_by_cms: 'url'
        });

        convertToNewFormat({ query }, null, () => {});

        expect(query).toEqual(expectedResult);
    });

    test('should convert fields extracted by partner-website', () => {
        const query = Object.assign({}, defaultQuery, { method: 'partner-website', model_id: 123 });

        const expectedResult = Object.assign({}, query, {
            'name_by_partner-website': 'name',
            price_by_price: 'price',
            'currency_by_partner-website': 'currency',
            'url_by_partner-website': 'url',
            'model_id_by_partner-website': 123
        });

        convertToNewFormat({ query }, null, () => {});

        expect(query).toEqual(expectedResult);
    });
});
