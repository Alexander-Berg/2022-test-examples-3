'use strict';

const sinon = require('sinon');
const Ajv = require('ajv');

const Client = require('./../../../lib/client');
const requests = require('./../../../data/products-requests');
const responses = require('./../../../data/products-responses');
const PRODUCT_ROUTE = require('../../../lib/routes').PRODUCTS;

const ajv = new Ajv();

const TRANSCEND_MOCK_RULES = [
    Client.API_MARKET_MOCKS.CATEGORY_MATCH.TRANSCEND,
    Client.API_MARKET_MOCKS.SHOPS.OZON,
    Client.API_MARKET_MOCKS.SEARCH_BY_TEXT.TRANSCEND,
    Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
    Client.API_MARKET_MOCKS.MODEL_OFFERS.TRANSCEND_JSI,
    Client.API_MARKET_MOCKS.MODEL_OUTLETS.TRANSCEND,
    Client.API_MARKET_MOCKS.MODEL.TRANSCEND,
    Client.API_MARKET_MOCKS.MODEL_MATCH.EMPTY,
];

const ROSES_MOCK_RULES = [
    Client.API_MARKET_MOCKS.CATEGORY_MATCH.FEATHER_ROSES,
    Client.API_MARKET_MOCKS.SHOPS.EMPTY,
    Client.API_MARKET_MOCKS.SEARCH_BY_TEXT.EMPTY,
    Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
    Client.API_MARKET_MOCKS.MODEL_OFFERS.EMPTY,
    Client.API_MARKET_MOCKS.MODEL_OUTLETS.EMPTY,
    Client.API_MARKET_MOCKS.CATEGORY.FEATHER_ROSES,
];

// TODO: rewrite
describe('Sovetnik with JS-integration', () => {
    afterEach(() => {
        Date.now.restore && Date.now.restore();
    });

    test('should get an information about out in', async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        const isOptIn = ajv.validate(responses.SETTINGS.OPT_IN, result.response.settings);
        expect(isOptIn).toBeFalsy();

        const isOptOut = ajv.validate(responses.SETTINGS.OPT_OUT, result.response.settings);
        expect(isOptOut).toBeTruthy();
    });

    test.skip('should save the timestamp of showing opt-in', async () => {
        const date = new Date(2016, 10, 10);

        sinon.stub(Date, 'now').returns(date.getTime());

        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        expect(result.getCookieValue('svt-user').lastOptInShowTime).toBe(date.getTime());
    });

    test.skip('should return "opt-in-interval"-rule if next request was sent after 10 minutes', async () => {
        const date = new Date(2016, 10, 10);
        const diff = 10 * 60 * 1000;

        sinon.stub(Date, 'now').returns(date.getTime());

        let client = new Client();
        let result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        Date.now.returns(date.getTime() + diff);

        const userSettings = result.getCookieValue('svt-user');
        const partnerSettings = result.getCookieValue('svt-partner');
        client = new Client(partnerSettings, userSettings);
        result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);
        const isOptInIntervalRule = ajv.validate(responses.COMMON.OPT_IN_INTERVAL, result.response);

        expect(isOptInIntervalRule).toBeTruthy();
    });

    test.skip('should return "opt-in-interval"-rule if next request was sent after 28 minutes', async () => {
        const date = new Date(2016, 10, 10);
        const diff = 28 * 60 * 1000;

        sinon.stub(Date, 'now').returns(date.getTime());

        let client = new Client();
        let result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        Date.now.returns(date.getTime() + diff);

        const userSettings = result.getCookieValue('svt-user');
        const partnerSettings = result.getCookieValue('svt-partner');
        client = new Client(partnerSettings, userSettings);
        result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);
        const isOptInIntervalRule = ajv.validate(responses.COMMON.OPT_IN_INTERVAL, result.response);

        expect(isOptInIntervalRule).toBeTruthy();
    });

    test("shouldn't return 'opt-in-interval-rule' if was not found offers", async () => {
        const client = new Client();
        const result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.FEATHER_ROSES, ...ROSES_MOCK_RULES);
        const isLastOptInShowTime = ajv.validate(responses.SETTINGS.LAST_OPT_IN_SHOW_TIME, result.response.settings);

        expect(isLastOptInShowTime).toBeFalsy();
    });

    test('should return response and information about opt-out if next request was sent after 35 minutes', async () => {
        const date = new Date(2016, 10, 10);
        const diff = 35 * 60 * 1000;

        sinon.stub(Date, 'now').returns(date.getTime());

        let client = new Client();
        let result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        Date.now.returns(date.getTime() + diff);

        const partnerSettings = result.getCookieValue('svt-partner');
        client = new Client(partnerSettings);
        result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        const isOptIn = ajv.validate(responses.SETTINGS.OPT_IN, result.response.settings);
        expect(isOptIn).toBeFalsy();

        const isOptOut = ajv.validate(responses.SETTINGS.OPT_OUT, result.response.settings);
        expect(isOptOut).toBeTruthy();
    });

    test('should return response without rules if user have accepted opt-in', async () => {
        const date = new Date(2016, 10, 10);
        const diff = 5 * 60 * 1000;

        sinon.stub(Date, 'now').returns(date.getTime());

        let client = new Client();
        let result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        Date.now.returns(date.getTime() + diff);

        const partnerSettings = result.getCookieValue('svt-partner');
        partnerSettings.optOutAccepted = true;
        client = new Client(partnerSettings);
        result = await client.request(PRODUCT_ROUTE, requests.JS_INTEGRATION.TRANSCEND, ...TRANSCEND_MOCK_RULES);

        expect(result.response.rules).toBeFalsy();
    });
});
