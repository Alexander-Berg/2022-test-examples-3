'use strict';

const Client = require('./../../lib/client');
const settings = require('./../../data/settings');
const requests = require('./../../data/products-requests');
const PRODUCT_ROUTE = require('../../lib/routes').PRODUCTS;

// TODO: rewrite
describe('Should not return second-script with the same clid but new vid(old-456, new-123)', () => {
    test('Returning correct response with changed vid', async () => {
        const client = new Client(settings.PARTNERS.SOVETNIK_WITH_VID, settings.USERS.SOVETNIK_WITH_VID);
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

        expect(result.response.rules).toBeFalsy();
    });

    test('Returning correct response with added vid', async () => {
        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.SOVETNIK);
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

        expect(result.response.rules).toBeFalsy();
    });
});
