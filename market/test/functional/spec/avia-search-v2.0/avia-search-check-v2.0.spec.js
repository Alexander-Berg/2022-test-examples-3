const Ajv = require('ajv');

const Client = require('./../../lib/client');
const AVIA_REQUESTS = require('./../../data/avia-requests');
const AVIA_RESPONSES = require('./../../data/avia-responses');
const LOGS = require('./../../data/logs');
const { AVIA_SEARCH_CHECK_V2 } = require('../../lib/routes');

const ajv = new Ajv();

describe('Avia search check v2.0', () => {
    describe("requests to 'https//api.avia.yandex.net' are mocked", () => {
        describe('components', () => {
            describe('aviabar component', () => {
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
                    const actual = await client.request(AVIA_SEARCH_CHECK_V2, AVIA_REQUESTS.CHECK.COUNT_0, mock);

                    let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.CHECK_V2.EMPTY_SCHEMA, actual.response);
                    expect(isValidate).toBeTruthy();

                    isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
                    expect(isValidate).toBeTruthy();
                });

                test('should return aviabar component data', async () => {
                    const mock = {
                        hostname: 'https://api.avia.yandex.net',
                        path: '/sovetnik/v1.0/search/results/',
                        query: { search_id: 'mock search id' },
                        result: AVIA_RESPONSES.API_AVIA.RESULTS.DME_BCN,
                    };

                    const client = new Client();
                    const actual = await client.request(AVIA_SEARCH_CHECK_V2, AVIA_REQUESTS.CHECK.COUNT_0, mock);

                    let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.CHECK_V2.FULL_SCHEMA, actual.response);
                    expect(isValidate).toBeTruthy();

                    isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
                    expect(isValidate).toBeTruthy();
                });
            });
        });
    });
});
