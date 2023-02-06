'use strict';

const Ajv = require('ajv');

const SETTINGS = require('./../../../data/settings');
const PRODUCT_REQUESTS = require('./../../../data/products-requests');
const EXPECTED_RESPONSES = require('./../../../data/products-responses');
const Client = require('./../../../lib/client');
const PRODUCT_ROUTE = require('../../../lib/routes').PRODUCTS;

const ajv = new Ajv();

// TODO: rewrite
describe('request from extension with button', () => {
    const MOCK_RULES = [
        Client.API_MARKET_MOCKS.CATEGORY_MATCH.EMPTY,
        Client.API_MARKET_MOCKS.SHOPS.EMPTY,
        Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
        Client.API_MARKET_MOCKS.MODEL_MATCH.TRANSCEND,
        Client.API_MARKET_MOCKS.MODEL.TRANSCEND,
        Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
        Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND,
    ];

    describe('user with SaveFrom and ButtonExtension', () => {
        test('should return second-script and response', async () => {
            const client = new Client(SETTINGS.PARTNERS.SAVE_FROM);
            const requestResult = await client.request(
                PRODUCT_ROUTE,
                PRODUCT_REQUESTS.BUTTON_EXTENSION.TRANSCEND,
                ...MOCK_RULES,
            );
            const response = requestResult.response;

            expect(ajv.validate(EXPECTED_RESPONSES.COMMON.SECOND_SCRIPT, response)).toBeTruthy();
            expect(response.searchInfo).toBeTruthy();
        });

        test('should log that result can be shown only inside button popup', async () => {
            const client = new Client(SETTINGS.PARTNERS.SAVE_FROM);
            const requestResult = await client.request(PRODUCT_ROUTE, PRODUCT_REQUESTS.BUTTON_EXTENSION.TRANSCEND);
            const log = requestResult.logs.product;

            expect(log.second_script_button).toBe(1);
        });

        test('should log that it is request from extension with button', async () => {
            const client = new Client(SETTINGS.PARTNERS.SAVE_FROM);
            const requestResult = await client.request(PRODUCT_ROUTE, PRODUCT_REQUESTS.BUTTON_EXTENSION.TRANSCEND);
            const log = requestResult.logs.product;

            expect(log.user_with_button).toBe(1);
        });
    });
});
