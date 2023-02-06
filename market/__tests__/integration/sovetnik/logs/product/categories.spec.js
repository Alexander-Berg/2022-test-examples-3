/* eslint-disable max-len */

const lib = require('../../../../lib');
const routes = require('../../../../../routes').default;
const logs = require('../../../../../utils/logs');

describe.skip('sovetnik / logs / product', () => {
    beforeAll(() => {
        logs.trackSettings = jest.fn(() => undefined);
        logs.trackDomainData = jest.fn(() => undefined);
        logs.trackDisableReason = jest.fn(() => undefined);
        // logs.trackExternalApiCall = jest.fn(() => undefined);
    });

    afterAll(() => {
        logs.trackSettings.mockReset();
        logs.trackSettings.mockRestore();

        logs.trackDomainData.mockReset();
        logs.trackDomainData.mockRestore();

        logs.trackDisableReason.mockReset();
        logs.trackDisableReason.mockRestore();

        // logs.trackExternalApiCall.mockReset();
        // logs.trackExternalApiCall.mockRestore();
    });

    test('should contain category identifiers', async () => {
        const client = new lib.Client(lib.stub.settings.partner.sovetnik, lib.stub.settings.user.sovetnik);
        const response = await client.request(
            routes.products,
            lib.stub.requests.product['Sovetnik | Apple iPhone X 256Gb | e-katalog.ru'],
            lib.mock.API.yandex.market['v1.0.0'].category.match['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v1.0.0'].shops['e-katalog.ru | 200'],
            lib.mock.API.yandex.market['v2.1.0'].search['Apple iPhone X 256Gb | e-katalog.ru | 200 | (1)'],
            lib.mock.API.yandex.market['v2.1.0'].search['Apple iPhone X 256Gb | e-katalog.ru | 200 | (2)'],
            lib.mock.API.yandex.market['v2.0.0'].models.match['Apple-iPhone-X-256Gb'],
            lib.mock.API.yandex.market['v2.1.4'].model['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v1.0.0'].model.outlets['Apple iPhone X 256Gb | 200'],
            lib.mock.API.yandex.market['v2.1.0'].models.offers['Apple-iPhone-X-256GB'],
        );

        const logs = response.logs.product;

        expect(logs).toHaveProperty('category_id', 91491);
        expect(logs).toHaveProperty('category_path', '90401,198119,91461,91491');
        expect(logs).toHaveProperty('category_formed_with', 'classifier');
        expect(logs).toHaveProperty('root_category_id', '198119');
        expect(logs).toHaveProperty('offers_category_id', '91491,91491,91491,91491,91491,91491');
        expect(logs).toHaveProperty(
            'offers_category_path',
            '[\\"90401,198119,91461,91491\\",\\"90401,198119,91461,91491\\",\\"90401,198119,91461,91491\\",\\"90401,198119,91461,91491\\",\\"90401,198119,91461,91491\\",\\"90401,198119,91461,91491\\"]',
        );
    });
});
