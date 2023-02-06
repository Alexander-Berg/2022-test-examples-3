'use strict';

const extractProductFields = require('./../../../middleware/prepare-query/extract-product-fields');
const sortProductFields = require('./../../../middleware/prepare-query/sort-product-fields');

describe('extract product fields and sort them', () => {
    test('should extract product fields and range it right', () => {
        const query = {
            name_by_cms: 'nameByCMS',
            name_by_us: 'nameByUserSelection',
            price_by_cms: 'priceByCMS',
            currency_by_cms: 'currencyByCMS',
            url_by_cms: 'urlByCMS',
            name_by_md: 'nameByMicrodata',
            name_by_dl: 'nameByDataLayer',
            vendor_by_md: 'vendorByMicrodata',
            model_id_by_pw: 'modelIdByPartnerWebsite',
            name_by_pw: 'nameByPartnerWebsite',
            name_by_sr: 'nameBySearch',
            name_by_sl: 'nameBySelectors',
            category_by_sl: 'categoryBySelectors',
            url_by_sh: 'urlByShop',
            url_by_rs: 'urlByReviewSite',
            price_by_df: 'priceByDefaultSelectors',
            category_by_dl: 'categoryByDl',
            vendor_by_sv: 'vendorBySpecifiedValues',
            price_by_partner: 'priceByPartner',
            vendor_by_partner: 'vendorByPartner',
            price_by_us: 'priceByUserSelection'
        };

        const expectedResult = {
            name: [
                { value: 'nameByUserSelection', method: 'user-selection', field: 'name' },
                { value: 'nameByPartnerWebsite', method: 'partner-website', field: 'name' },
                { value: 'nameBySelectors', method: 'selectors', field: 'name' },
                { value: 'nameByDataLayer', method: 'data-layer', field: 'name' },
                { value: 'nameByMicrodata', method: 'microdata', field: 'name' },
                { value: 'nameByCMS', method: 'cms', field: 'name' },
                { value: 'nameBySearch', method: 'search', field: 'name' }
            ],
            price: [
                { value: 'priceByUserSelection', method: 'user-selection', field: 'price' },
                { value: 'priceByCMS', method: 'cms', field: 'price' },
                { value: 'priceByDefaultSelectors', method: 'default-selectors', field: 'price' },
                { value: 'priceByPartner', method: 'partner', field: 'price' }
            ],
            currency: [{ value: 'currencyByCMS', method: 'cms', field: 'currency' }],
            url: [
                { value: 'urlByCMS', method: 'cms', field: 'url' },
                { value: 'urlByShop', method: 'shop', field: 'url' },
                { value: 'urlByReviewSite', method: 'review-site', field: 'url' }
            ],
            vendor: [
                { value: 'vendorBySpecifiedValues', method: 'specified-values', field: 'vendor' },
                { value: 'vendorByMicrodata', method: 'microdata', field: 'vendor' },
                { value: 'vendorByPartner', method: 'partner', field: 'vendor' }
            ],
            model_id: [{ value: 'modelIdByPartnerWebsite', method: 'partner-website', field: 'model_id' }],
            category: [
                { value: 'categoryBySelectors', method: 'selectors', field: 'category' },
                { value: 'categoryByDl', method: 'data-layer', field: 'category' }
            ]
        };

        const req = { query };
        extractProductFields(req, null, () => {});
        sortProductFields(req, null, () => {});

        expect(req.productFields).toEqual(expectedResult);
    });

    test('should extract product fields with only df fields', () => {
        const query = {
            h1_by_df: 'h1ByDf',
            price_by_df: '9000'
        };

        const expectedResult = {
            h1: [{ value: 'h1ByDf', method: 'default-selectors', field: 'h1' }],
            name: [{ value: 'h1ByDf', method: 'default-selectors', field: 'name' }],
            price: [{ value: '9000', method: 'default-selectors', field: 'price' }]
        };

        const req = { query };
        extractProductFields(req, null, () => {});
        sortProductFields(req, null, () => {});

        expect(req.productFields).toEqual(expectedResult);
    });
});
