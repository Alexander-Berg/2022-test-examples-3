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
const shopsMock: Mock = require('./__mocks__/shops.offers-by-cbir');
const categoriesMatchMock: Mock = require('./__mocks__/categories-match.offers-by-cbir');
const cbirMock: Mock = require('./__mocks__/cbir-market.offers-by-cbir');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: 'ea7bcbbd88893210b697016578106cb3',
    v: '201910231216',
    isSelectorExists: true,
    transaction_id: 'unknown-k23p7oo3fo199zh10dcqdonmoczyff60',
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
    screen_size: { width: 1921, height: 876 },
    screen_resolution: { ratio: 1, width: 1921, height: 1019 },
    viewport: { width: 1904, height: 876 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: false,
    url: 'https://www.fmarconi.ru/catalog/women/sumka_zhenskaja_84520mg_carmen_nero/',
    pictures_by_tg: ['http://www.fmarconi.ru/upload/iblock/a13/a13f017907e50180e1873aa9dfe2a62f.jpg', ''],
    h1_by_df: 'Francesco Marconi 84520mg carmen nero',
    name_by_sl: 'Francesco Marconi 84520mg carmen nero',
    pictures_by_sl: [
        '//opt-480797.ssl.1c-bitrix-cdn.ru/upload/resize_cache/iblock/3d9/800_800_175511db9cefbc414a902a46f1b8fae16/3d97201cb62f3e69fcd586082a6e718f.jpg?150814547355638',
    ],
    category_by_sl: 'Для нее/Сумки женские/Сумки классические',
    price_by_sl: 9620,
    url_by_sh: 'https://www.fmarconi.ru/catalog/women/sumka_zhenskaja_84520mg_carmen_nero/',
    name_by_og: 'Сумка женская Francesco Marconi carmen nero',
    self_by_sd: false,
};

describe.skip('Search pipeline: offers-by-cbir', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(laasMock, yabroFilterMock, shopsMock, categoriesMatchMock, cbirMock);
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
