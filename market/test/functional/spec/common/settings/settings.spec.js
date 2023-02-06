const Ajv = require('ajv');

const Client = require('../../../lib/client');
const PRODUCT_ROUTE = require('../../../lib/routes').PRODUCTS;
const SETTINGS = require('../../../data/settings');
const PRODUCT_REQUESTS = require('../../../data/products-requests');
const EXPECTED_RESPONSES = require('../../../data/products-responses');

const ajv = new Ajv();

// TODO: rewrite
describe('tapki', () => {
    describe('second script', () => {
        test("should return 'second-script' (partner - SaveFrom, user - Sovetnik, request - Sovetnik)", async () => {
            const client = new Client(SETTINGS.PARTNERS.SAVE_FROM, SETTINGS.USERS.SOVETNIK);
            const requestResult = await client.request(PRODUCT_ROUTE, PRODUCT_REQUESTS.SOVETNIK.IPHONE);
            const { response } = requestResult;

            expect(ajv.validate(EXPECTED_RESPONSES.COMMON.SECOND_SCRIPT, response)).toBeTruthy();
        });
    });

    describe.skip('response', () => {
        test('should return correct response (partner - SaveFrom, user - SaveFrom, request - SaveFrom)', async () => {
            const client = new Client(SETTINGS.PARTNERS.SAVE_FROM, SETTINGS.USERS.SAVE_FROM);

            const requestResult = await client.request(
                PRODUCT_ROUTE,
                PRODUCT_REQUESTS.SAVEFROM.IPHONE,
                Client.API_MARKET_MOCKS.CATEGORY_MATCH.IPHONE,
                Client.API_MARKET_MOCKS.SHOPS.EMPTY,
                Client.API_MARKET_MOCKS.SEARCH_BY_URL.EMPTY,
                Client.API_MARKET_MOCKS.MODEL_MATCH.IPHONE,
                Client.API_MARKET_MOCKS.MODEL.IPHONE,
                Client.API_MARKET_MOCKS.MODEL_OUTLETS.EMPTY,
                Client.API_MARKET_MOCKS.CATEGORY.EMPTY,
            );

            const { response } = requestResult;
            let result = ajv.validate(EXPECTED_RESPONSES.IPHONE.MODEL, response.model);

            expect(result).toBeTruthy();

            result = ajv.validate(EXPECTED_RESPONSES.IPHONE.SETTINGS, response.settings);
            expect(result).toBeTruthy();

            expect(ajv.validate(EXPECTED_RESPONSES.IPHONE.SEARCH_INFO, response.searchInfo)).toBeTruthy();

            if (response && response.shopInfo) {
                expect(ajv.validate(EXPECTED_RESPONSES.IPHONE.SHOP_INFO, response.shopInfo)).toBeTruthy();
            }
        });
    });
});
