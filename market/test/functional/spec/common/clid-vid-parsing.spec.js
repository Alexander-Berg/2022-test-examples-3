'use strict';

const Client = require('./../../lib/client');
const requests = require('./../../data/products-requests');
const settings = require('./../../data/settings');
const PRODUCT_ROUTE = require('../../lib/routes').PRODUCTS;

// TODO: rewrite
describe('Should parse given clid and vid correctly', () => {
    test('Parsing pair clid-vid 2210590-123 correctly', async () => {
        const client = new Client(settings.USERS.SOVETNIK);
        const result = await client.request(
            PRODUCT_ROUTE,
            requests.SOVETNIK.IPHONE_WITH_VID,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
            Client.API_MARKET_MOCKS.MODEL.IPHONE,
            Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
            Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
        );

        expect(result.logs.product.clid).toBe('2210590');
        expect(result.logs.product.vid).toBe('123');
    });
});
