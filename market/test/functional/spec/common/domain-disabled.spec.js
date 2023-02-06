'use strict';

const Client = require('./../../lib/client');
const settings = require('./../../data/settings');
const requests = require('./../../data/products-requests');
const PRODUCT_ROUTE = require('../../lib/routes').PRODUCTS;

// TODO: rewrite
describe('Domain disabled', () => {
    test('User with Sovetnik disabled on eldorado.ru should not get offers', async () => {
        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.DOMAIN_DISABLED);

        const result = await client.request(
            PRODUCT_ROUTE,
            requests.SOVETNIK.ELDORADO_REQUEST,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
            Client.API_MARKET_MOCKS.MODEL.IPHONE,
            Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
            Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
        );

        expect(result.response.rules).toEqual(['domain-disabled']);
    });
});
