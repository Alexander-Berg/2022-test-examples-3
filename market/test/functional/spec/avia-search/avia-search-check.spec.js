const Ajv = require('ajv');

const Client = require('./../../lib/client');
const AVIA_REQUESTS = require('./../../data/avia-requests');
const AVIA_RESPONSES = require('./../../data/avia-responses');
const LOGS = require('./../../data/logs');
const { AVIA_SEARCH_CHECK } = require('../../lib/routes');

const ajv = new Ajv();

describe('Avia search check', () => {
    describe("requests to 'https//api.avia.yandex.net' are mocked", () => {
        test('should return full result', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.DME_BCN,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.CHECK.FULL_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.CHECK, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test('should not return error if response consists BYR-currency', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.CURRENCIES.BYR,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.CHECK.FULL_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.CHECK, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test("should return full result (variants without 'backward'-field)", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.DME_NUE,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.CHECK.FULL_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.CHECK, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test('should return empty result (variant, flights, companies are empty)', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.EMPTY,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.CHECK.EMPTY_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test("should return data without 'error'-field (status code is 504)", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS[504],
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);
            const isValidate = ajv.validate(LOGS.AVIA.SEARCH.ERROR, actual.logs && actual.logs.avia);

            expect(actual.response.error).toBeUndefined();
            expect(isValidate).toBeFalsy();
        });

        test("should return error (without 'count'-parameter)", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.DME_NUE,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.WITHOU_COUNT, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.ERROR_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.ERROR, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test('should return empty result if flight key are not consistent', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.FLIGHTS_KEY_ARE_NOT_CONSISTENCY,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.ERROR_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.ERROR, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test('response should contain correct notification data', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.DME_BCN,
            };

            const client = new Client(Client.SETTINGS.PARTNERS.SOVETNIK, Client.SETTINGS.USERS.SOVETNIK, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);
            const isValidate = ajv.validate(
                AVIA_RESPONSES.SEARCH.CHECK.NOTIFICATION_DME_BCN_SCHEMA,
                actual.response.notification,
            );

            expect(isValidate).toBeTruthy();
        });

        test('should return empty result if partners are not consistent', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/results/',
                query: {
                    search_id: 'mock search id',
                },
                result: AVIA_RESPONSES.API_AVIA.RESULTS.PARTNERS_KEY_ARE_NOT_CONSISTENCY,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_CHECK, AVIA_REQUESTS.CHECK.COUNT_0, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.ERROR_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.ERROR, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });
    });
});
