'use strict';

const Ajv = require('ajv');

const Client = require('./../../lib/client');
const settings = require('./../../data/settings');
const requests = require('./../../data/products-requests');
const EXPECTED_RESPONSE = require('./../../data/products-responses');
const PRODUCT_ROUTE = require('../../lib/routes').PRODUCTS;

const ajv = new Ajv();

describe.skip('Checking if we can get offers', () => {
    test('User with classic Sovetnik can get offers', async () => {
        const client = new Client(settings.USERS.SOVETNIK);

        const result = await client.request(
            PRODUCT_ROUTE,
            requests.SOVETNIK.OFFERS_MOUSE_PAD,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.MOUSE_PAD,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_TEXT.MOUSE_PAD,
        );

        expect(ajv.validate(EXPECTED_RESPONSE.COMMON.OFFERS_MOUSE_PAD, result.response.offers[0])).toBeTruthy();
        // FIXME: (tests) fails when we run all tests
        expect(ajv.validate(EXPECTED_RESPONSE.COMMON.OFFERS_MOUSE_PAD_RAZER, result.response.offers[0])).toBeTruthy();
    });
});
