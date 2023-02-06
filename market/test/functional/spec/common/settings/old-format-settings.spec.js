'use strict';

const PRODUCT_ROUTE = require('../../../lib/routes').PRODUCTS;
const Client = require('./../../../lib/client');
const requests = require('./../../../data/products-requests');

// TODO: rewrite
describe('Old format requests (without settings parameter)', () => {
    test('Should log right clid and aff_id', async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.SAVEFROM.IPHONE_OLD_FORMAT);
        const log = result.logs.product;

        expect(log.clid).toBe(2210496);
        expect(log.aff_id).toBe(1);
    });
});
