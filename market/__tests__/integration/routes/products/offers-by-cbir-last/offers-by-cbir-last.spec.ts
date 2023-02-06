import supertest from 'supertest';
import nock from 'nock';

import app from '../../../../../src/app';
import raiseMocks from '../../../helpers/raise-mocks';
import Mock from '../../../../types/mock';

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const yabroFilterMock: Mock = require('../../__mocks__/yabro-filter');
const laasMock: Mock = require('../../__mocks__/laas.mock');
const shopsMock: Mock = require('./__mocks__/shops.offers-by-cbir-last');
const modelsMatchMock: Mock = require('../__mocks__/empty-model-match.mock');
const categoriesMatchMock: Mock = require('./__mocks__/categories-match.offers-by-cbir-last');
const categoriesIdSearchMock: Mock = require('./__mocks__/categories-search.offers-by-cbir-last');
const redirectMock: Mock = require('./__mocks__/redirect.offers.by-cbir-last');
const cbirMock: Mock = require('./__mocks__/cbir-market.offers-by-cbir-last');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: '269d55cf3067ae1513ff4e211f88801b',
    v: '201910231216',
    isSelectorExists: true,
    transaction_id: 'k282h6h4pvmvdvmpe6snetyro5t8dpmy',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1571856896087',
        installId: '0e26a405-74be-4ae5-984a-1cda3d838edc',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: { width: 1920, height: 876 },
    screen_resolution: { ratio: 1, width: 1920, height: 1019 },
    viewport: { width: 1887, height: 1789 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: false,
    url:
        'https://meshok.net/item/142741120_%D0%9F%D0%9E%D0%A0%D0%A2%D0%A3%D0%93%D0%90%D0%9B%D0%98%D0%AF_1000_%D0%AD%D0%A1%D0%9A%D0%A3%D0%94%D0%9E_1996%D0%B3_%D0%A1%D0%95%D0%A0%D0%95%D0%91%D0%A0%D0%9E_%D0%9A%D0%A0%D0%90%D0%A1%D0%98%D0%92%D0%90%D0%AF',
    pictures_by_tg: ['https://pics.meshok.net/pics/142741120.jpg?1'],
    h1_by_df: 'ПОРТУГАЛИЯ, 1000 ЭСКУДО 1996г., СЕРЕБРО! КРАСИВАЯ!',
    price_by_md: 201,
    currency_by_md: 'RUB',
    name_by_md: 'ПОРТУГАЛИЯ, 1000 ЭСКУДО 1996г., СЕРЕБРО! КРАСИВАЯ!',
    pictures_by_md: ['https://pics.meshok.net/pics/142741120.jpg?1'],
    name_by_sl: 'ПОРТУГАЛИЯ, 1000 ЭСКУДО 1996г., СЕРЕБРО! КРАСИВАЯ!',
    price_by_sl: 201,
    url_by_sh:
        'https://meshok.net/item/142741120_%D0%9F%D0%9E%D0%A0%D0%A2%D0%A3%D0%93%D0%90%D0%9B%D0%98%D0%AF_1000_%D0%AD%D0%A1%D0%9A%D0%A3%D0%94%D0%9E_1996%D0%B3_%D0%A1%D0%95%D0%A0%D0%95%D0%91%D0%A0%D0%9E_%D0%9A%D0%A0%D0%90%D0%A1%D0%98%D0%92%D0%90%D0%AF',
    self_by_sd: false,
};

describe.skip('Search pipeline: offers-by-cbir-last', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            shopsMock,
            modelsMatchMock,
            categoriesIdSearchMock,
            redirectMock,
            cbirMock,
            categoriesMatchMock,
        );
    });

    afterAll(() => {
        nock.cleanAll();
    });

    test('should find offers', async () => {
        const response: any = await request
            .post('/products')
            .set('X-HTTP-Method-Override', 'GET')
            .send(body);

        const { statusCode, text } = response;
        const responseBody = JSON.parse(text);

        expect(statusCode).toBe(200);
        expect(responseBody).toEqual(responseStub);

        mockScopes.forEach((scope) => scope.done());
    });
});
