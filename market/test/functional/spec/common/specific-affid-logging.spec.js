'use strict';

/**
 * @see {@link https://st.yandex-team.ru/SOVETNIK-10409}
 */

const Client = require('./../../lib/client');
const settings = require('./../../data/settings');
const requests = require('./../../data/products-requests');
const PRODUCT_ROUTE = require('../../lib/routes').PRODUCTS;

// TODO: rewrite
describe('Checking correct aff_id logging', () => {
    test('Aff_id 1020 is logged 1', async () => {
        const PARTNER_AFF_ID_1020 = JSON.parse(JSON.stringify(requests.SOVETNIK.IPHONE));
        PARTNER_AFF_ID_1020.query.settings.affId = 1020;

        const client = new Client(settings.USERS.SOVETNIK);
        const result = await client.request(
            PRODUCT_ROUTE,
            PARTNER_AFF_ID_1020,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
            Client.API_MARKET_MOCKS.MODEL.IPHONE,
            Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
            Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
        );

        expect(result.logs.product.aff_id).toBe(1);
    });

    test('Aff_id 1025 is logged 1', async () => {
        const PARTNER_AFF_ID_1020 = JSON.parse(JSON.stringify(requests.SOVETNIK.IPHONE));
        PARTNER_AFF_ID_1020.query.settings.affId = 1025;

        const client = new Client(settings.USERS.SOVETNIK);
        const result = await client.request(
            PRODUCT_ROUTE,
            PARTNER_AFF_ID_1020,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
            Client.API_MARKET_MOCKS.MODEL.IPHONE,
            Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
            Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
        );

        expect(result.logs.product.aff_id).toBe(1);
    });

    test('Aff_id 1037 is logged 1', async () => {
        const PARTNER_AFF_ID_1025 = JSON.parse(JSON.stringify(requests.SOVETNIK.IPHONE));
        PARTNER_AFF_ID_1025.query.settings.affId = 1037;

        const client = new Client(settings.USERS.SOVETNIK);
        const result = await client.request(
            PRODUCT_ROUTE,
            PARTNER_AFF_ID_1025,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
            Client.API_MARKET_MOCKS.MODEL.IPHONE,
            Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
            Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
        );

        expect(result.logs.product.aff_id).toBe(1);
    });

    test('Aff_id 2 is logged 1300', async () => {
        const PARTNER_AFF_ID_2 = JSON.parse(JSON.stringify(requests.SOVETNIK.IPHONE));
        PARTNER_AFF_ID_2.query.settings.affId = 2;

        const client = new Client(settings.USERS.SOVETNIK);
        const result = await client.request(
            PRODUCT_ROUTE,
            PARTNER_AFF_ID_2,
            Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
            Client.API_MARKET_MOCKS.SHOPS.EMPTY,
            Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
            Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
            Client.API_MARKET_MOCKS.MODEL.IPHONE,
            Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
            Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
        );

        expect(result.logs.product.aff_id).toBe(1300);
    });
});
