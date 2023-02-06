'use strict';

const Ajv = require('ajv');

const Client = require('./../../lib/client');
const settings = require('./../../data/settings');
const requests = require('./../../data/products-requests');
const EXPECTED_RESPONSE = require('./../../data/products-responses');
const PRODUCT_ROUTE = require('../../lib/routes').PRODUCTS;

const ajv = new Ajv();

// TODO: rewrite
describe.skip('Checking if we can get searchResult(mixed popup)', () => {
    // FIXME
    test.skip('User with classic Sovetnik can get searchResult with models', async () => {
        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.SOVETNIK, true);

        const result = await client.request(
            PRODUCT_ROUTE,
            requests.SOVETNIK.SEARCH_RESULT_MODELS,

            Client.API_MARKET_MOCKS.REDIRECT.TABLET,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.TABLET,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.FILTER.TABLET,
            Client.API_MARKET_MOCKS.SEARCH_BY_TEXT.TABLET,
            Client.API_MARKET_MOCKS.CATEGORY.TABLET,
        );

        expect(ajv.validate(EXPECTED_RESPONSE.COMMON.SEARCH_RESULT, result.response.searchResult[0])).toBeTruthy();
    });

    test.skip('User with classic Sovetnik can get searchResult with offers', async () => {
        const client = new Client(settings.PARTNERS.SOVETNIK, settings.USERS.SOVETNIK, true);

        const result = await client.request(
            PRODUCT_ROUTE,
            requests.SOVETNIK.SEARCH_RESULT_OFFERS,
            Client.API_MARKET_MOCKS.REDIRECT.ADAPTER,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.ADAPTER,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_TEXT.ADAPTER,
            Client.API_MARKET_MOCKS.MODEL_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.FILTER.ADAPTER,
        );

        expect(ajv.validate(EXPECTED_RESPONSE.COMMON.OFFERS_MOUSE_PAD, result.response.searchResult[0])).toBeTruthy();
    });
});
