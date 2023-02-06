'use strict';

const Ajv = require('ajv');

const Client = require('./../../../lib/client');
const settings = require('./../../../data/settings');
const requests = require('./../../../data/products-requests');
const expectedLogs = require('./../../../data/logs');
const PRODUCT_ROUTE = require('../../../lib/routes').PRODUCTS;

const ajv = new Ajv();

// TODO: rewrite
describe('Sovetnik for sites', () => {
    test('User with SF-settings should not get a second script rule', async () => {
        const client = new Client(settings.PARTNERS.SAVE_FROM);
        const result = await client.request(PRODUCT_ROUTE, requests.S2S.ELECTROLUX_OTZOVIK);

        expect(result.response).toBeTruthy();
        expect(result.response.rules).toBeFalsy();
    });

    test('User with SF-settings should get "otzovik.com" in settings.applicationName', async () => {
        const client = new Client(settings.PARTNERS.SAVE_FROM);
        const result = await client.request(PRODUCT_ROUTE, requests.S2S.ELECTROLUX_OTZOVIK);

        expect(result.response.settings.applicationName).toBe('otzovik.com');
    });

    test('Should log correct params', async () => {
        const client = new Client(settings.PARTNERS.SAVE_FROM);
        const result = await client.request(PRODUCT_ROUTE, requests.S2S.ELECTROLUX_OTZOVIK);
        const logs = result.logs.product;
        const isLogValid = ajv.validate(expectedLogs.PRODUCTS.S2S_OTZOVIK_ELECTROLUX, logs);

        expect(isLogValid).toBeTruthy();
    });
});
