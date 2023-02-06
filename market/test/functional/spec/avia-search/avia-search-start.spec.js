const Ajv = require('ajv');

const Client = require('./../../lib/client');
const AVIA_REQUESTS = require('./../../data/avia-requests');
const AVIA_RESPONSES = require('./../../data/avia-responses');
const LOGS = require('./../../data/logs');
const { AVIA_SEARCH_START } = require('../../lib/routes');

const ajv = new Ajv();

describe('Avia search start', () => {
    describe("requests to 'https://api.avia.yandex.net' are mocked", () => {
        test("Avia API response without 'success'-'status'", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: {
                    status: 200,
                    body: {
                        status: 'fail',
                    },
                },
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);

            expect(actual.response.avia).toBeUndefined();
        });

        test("Avia API response without 'search_id'", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: {
                    status: 200,
                    body: {
                        status: 'success',
                        data: {
                            link: 'https://link.com/',
                            direction: {
                                arrival: {
                                    settlement: {
                                        iata: 'LOL',
                                        title: 'Arrival settlement title',
                                    },
                                },
                                departure: {
                                    settlement: {
                                        iata: 'OLO',
                                        title: 'Departure settlement title',
                                    },
                                },
                            },
                        },
                    },
                },
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);

            expect(actual.response.avia).toBeUndefined();
        });

        test("Avia API response without 'settlement'", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: {
                    status: 200,
                    body: {
                        status: 'success',
                        data: {
                            search_id: 'search id',
                            link: 'https://link.com/',
                            direction: {
                                arrival: {},
                                departure: {
                                    settlement: {
                                        iata: 'OLO',
                                        title: 'Departure settlement title',
                                    },
                                },
                            },
                        },
                    },
                },
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);

            expect(actual.response.avia).toBeUndefined();
        });

        test('Avia API response with status 500', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: {
                    status: 500,
                    body: {
                        status: 'success',
                        data: {
                            search_id: 'search id',
                            link: 'https://link.com/',
                            direction: {
                                arrival: {
                                    settlement: {
                                        iata: 'LOL',
                                        title: 'Arrival settlement title',
                                    },
                                },
                                departure: {
                                    settlement: {
                                        iata: 'OLO',
                                        title: 'Departure settlement title',
                                    },
                                },
                            },
                        },
                    },
                },
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);

            expect(actual.response.avia).toBeUndefined();
        });

        test("Avia API response with 'fail'-status", async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: {
                    status: 'fail',
                    data: {
                        description: {
                            errors: [
                                {
                                    help: {
                                        required: true,
                                        format: 'Внутренний код объекта из справочника, iata—код или сирена—код',
                                    },
                                    error: ['from', "Point not found u'MO'"],
                                },
                            ],
                        },
                    },
                },
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);

            expect(actual.response.avia).toBeUndefined();
        });

        test('Correct request for Avia API', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: AVIA_RESPONSES.API_AVIA.SEARCH.MOW_LED,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);

            let isValidate = ajv.validate(AVIA_RESPONSES.SEARCH.START.FULL_SCHEMA, actual.response);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.COMMON, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();

            isValidate = ajv.validate(LOGS.AVIA.SEARCH.START, actual.logs && actual.logs.avia);
            expect(isValidate).toBeTruthy();
        });

        test('should return correct departure and return dates', async () => {
            const mock = {
                hostname: 'https://api.avia.yandex.net',
                path: '/sovetnik/v1.0/search/',
                query: {
                    lang: 'ru',
                },
                result: AVIA_RESPONSES.API_AVIA.SEARCH.MOW_LED,
            };

            const client = new Client(null, null, false);
            const actual = await client.request(AVIA_SEARCH_START, AVIA_REQUESTS.SEARCH.DME_AER, mock);
            const expectedDepartDate = {
                date: 5,
                year: 2016,
                numberMonth: 10,
                shortMonth: 'окт',
                fullMonth: 'октябрь',
                text: '5 октября',
            };
            const expectedReturnDate = {
                date: 20,
                year: 2016,
                numberMonth: 11,
                shortMonth: 'ноя',
                fullMonth: 'ноябрь',
                text: '20 ноября',
            };

            expect(actual.response.avia.departDate).toEqual(expectedDepartDate);
            expect(actual.response.avia.returnDate).toEqual(expectedReturnDate);
        });
    });
});
