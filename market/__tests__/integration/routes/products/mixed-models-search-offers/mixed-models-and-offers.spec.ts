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
const shopsMock: Mock = require('./__mocks__/shops');
const searchEmptyMock: Mock = require('../../__mocks__/empty-url-search-report');
const categoriesMatchMock: Mock = require('./__mocks__/categories-match-mixed');
const modelMatchMock: Mock = require('./__mocks__/model-match.js');
const categoriesSearchMock: Mock = require('./__mocks__/offers-by-filter');
const modelOffersSingleMock: Mock = require('./__mocks__/models-offers');
const modelMock: Mock = require('./__mocks__/model');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: '1e1ef401159dc3f2cad0f1057103dad1',
    v: '201910212320',
    isSelectorExists: true,
    transaction_id: 'unknown-k22d2ydb2ve7js3d81d2b0tvwxmg3q65',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1571744341456',
        installId: '766664c8-432c-44a2-b35f-55bc033b8dea',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: { width: 1440, height: 644 },
    screen_resolution: { ratio: 1, width: 1440, height: 787 },
    viewport: { width: 1423, height: 4518 },
    notifications: 'chrome',
    ajax_session_id: 'k22d33d4s6si5p87fztgst4tjv9gsvp0',
    ui_language: 'ru',
    is_debug_mode: false,
    url: 'https://www.ozn.ru/context/detail/id/155289157/',
    pictures_by_tg: ['https://cdn1.ozone.ru/multimedia/1037176353.jpg'],
    name_by_sl: 'Хлебопечка Panasonic SD-2501WTS',
    pictures_by_sl: ['https://cdn1.ozone.ru/multimedia/c1200/1037176353.jpg'],
    category_by_sl: 'Автомобильные держатели/Baseus',
    price_by_sl: 2074,
    currency_by_sl: 'RUB',
    vendor_by_sl: 'Бренды',
    name_by_og:
        'Держатель с беспроводной зарядкой Baseus Big Ears — купить в интернет-магазине OZON с быстрой доставкой',
    self_by_sd: false,
};

describe.skip('Search pipeline: offers-by-filter', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            shopsMock,
            { ...searchEmptyMock, times: 2 },
            categoriesMatchMock,
            modelMatchMock,
            modelMock,
            modelOffersSingleMock,
            categoriesSearchMock,
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

        const { statusCode, text } = response as { statusCode: number; text: string };
        const responseBody = JSON.parse(text);

        expect(statusCode).toBe(200);
        expect(responseBody).toEqual(responseStub);

        mockScopes.forEach((scope) => scope.done());
    });
});
