const sinon = require('sinon');

const Client = require('../../../lib/client');
const PRODUCTS_ROUTE = require('../../../lib/routes').PRODUCTS;
const PRODUCTS_REQUESTS = require('../../../data/products-requests');

const CUSTOM_TEST_TIMEOUT_MS = 30000;

// TODO: rewrite
describe(
    'validators (yclid, ymclid)',
    () => {
        const toButtonRequest = (request) => {
            request = { manual: true, ...request.query };
            return { query: request };
        };

        describe('ymclid validator', () => {
            let dateNowStub;
            let dateGetFullYear;

            beforeAll(() => {
                dateNowStub = sinon.stub(Date, 'now', () => 1480658716576);
                dateGetFullYear = sinon.stub(Date.prototype, 'getFullYear', () => 2016);
            });

            afterAll(() => {
                dateNowStub && dateNowStub.restore();
                dateGetFullYear && dateGetFullYear.restore();
            });

            describe('request from button', () => {
                test('request with valid ymclid and valid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(PRODUCTS_ROUTE, toButtonRequest(PRODUCTS_REQUESTS.YMCLID.FULL));
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.ymclid).toBeDefined();
                    expect(logs.ymcliderror).toBeUndefined();
                });

                test.skip('request with shop error', async () => {
                    const client = new Client();
                    const actual = await client.request(
                        PRODUCTS_ROUTE,
                        toButtonRequest(PRODUCTS_REQUESTS.YMCLID.SHOP_ERROR),
                    );
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.ymclid).toBeDefined();
                    expect(logs.ymcliderror).toBeUndefined();
                });

                test('request with referrer error', async () => {
                    const client = new Client();
                    const actual = await client.request(
                        PRODUCTS_ROUTE,
                        toButtonRequest(PRODUCTS_REQUESTS.YMCLID.INVALID_REFERRER),
                    );
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.ymclid).toBeDefined();
                    expect(logs.ymcliderror).toBeUndefined();
                });
            });

            describe('request with undefined manual', () => {
                test('request with valid ymclid and valid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(PRODUCTS_ROUTE, PRODUCTS_REQUESTS.YMCLID.FULL);
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(1);
                    expect(logs.ymclid).toBeDefined();
                    expect(logs.ymcliderror).toBeUndefined();
                });

                test.skip('request with shop error', async () => {
                    const client = new Client();
                    const actual = await client.request(PRODUCTS_ROUTE, PRODUCTS_REQUESTS.YMCLID.SHOP_ERROR);
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.ymclid).toBeDefined();
                    expect(logs.ymcliderror).toBe('shop');
                });
            });
        });

        describe('yclid validator', () => {
            let dateNowStub;
            let dateGetFullYear;

            beforeAll(() => {
                dateNowStub = sinon.stub(
                    Date,
                    'now',
                    () => 1480680301560, // Fri Dec 02 2016 15:05:01 GMT+0300 (MSK)
                );
                dateGetFullYear = sinon.stub(Date.prototype, 'getFullYear', () => 2016);
            });

            afterAll(() => {
                dateNowStub && dateNowStub.restore();
                dateGetFullYear && dateGetFullYear.restore();
            });

            describe('request from button', () => {
                test('request with valid yclid and valid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(PRODUCTS_ROUTE, toButtonRequest(PRODUCTS_REQUESTS.YCLID.FULL));
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.yclid).toBeDefined();
                    expect(logs.ycliderror).toBeUndefined();
                });

                test('request with valid yclid and without referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(
                        PRODUCTS_ROUTE,
                        toButtonRequest(PRODUCTS_REQUESTS.YCLID.WITHOUT_REFERRER),
                    );
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.yclid).toBeDefined();
                    expect(logs.ycliderror).toBeUndefined();
                });

                test('request with valid yclid and with invalid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(
                        PRODUCTS_ROUTE,
                        toButtonRequest(PRODUCTS_REQUESTS.YCLID.INVALID_REFERRER),
                    );
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.yclid).toBeDefined();
                    expect(logs.ycliderror).toBeUndefined();
                });

                test('request with invalid yclid and with valid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(
                        PRODUCTS_ROUTE,
                        toButtonRequest(PRODUCTS_REQUESTS.YCLID.INVALID_YCLID),
                    );
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.yclid).toBeDefined();
                    expect(logs.ycliderror).toBeUndefined();
                });
            });

            describe('request with undefined manual', () => {
                test('request with valid yclid and valid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(PRODUCTS_ROUTE, PRODUCTS_REQUESTS.YCLID.FULL);
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(1);
                    expect(logs.yclid).toBeDefined();
                    expect(logs.ycliderror).toBeUndefined();
                });

                test('request with invalid yclid and with valid referrer', async () => {
                    const client = new Client();
                    const actual = await client.request(PRODUCTS_ROUTE, PRODUCTS_REQUESTS.YCLID.INVALID_YCLID);
                    const logs = actual.logs && actual.logs.product;

                    expect(logs.do_not_search).toBe(0);
                    expect(logs.yclid).toBeDefined();
                    expect(logs.ycliderror).toBe('format');
                });
            });
        });
    },
    CUSTOM_TEST_TIMEOUT_MS,
);
