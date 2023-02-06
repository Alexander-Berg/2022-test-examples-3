'use strict';

const Client = require('./../../functional/lib/client');
const PRODUCT_ROUTE = require('../../functional/lib/routes').PRODUCTS;
const requests = require('./../../functional/data/products-requests');

describe('init-search middleware', () => {
    test('returns an empty result for clid = 2306015', async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.SOVETNIK.IPHONE_CLID_AURORA);
        const clid = result.logs.product.clid;

        const {
            response: { searchInfo, settings, offers },
        } = result;

        expect(clid).toBe(2306015);
        expect(searchInfo).toBeDefined();
        expect(settings).toBeDefined();
        expect(offers).toBeUndefined();
    });

    test('should log do_not_search', async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.SOVETNIK.IPHONE_CLID_AURORA);
        const doNotSearch = result.logs.product.do_not_search;

        expect(doNotSearch).toBe(1);
    });

    test('should log do_not_search_reason=aurora', async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.SOVETNIK.IPHONE_CLID_AURORA);
        const doNotSearchReason = result.logs.product.do_not_search_reason;

        expect(doNotSearchReason).toBe('aurora');
    });

    test('should log all product data', async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.SOVETNIK.IPHONE_CLID_AURORA);
        const log = result.logs.product;

        expect(log).toBeDefined();
    });
});
