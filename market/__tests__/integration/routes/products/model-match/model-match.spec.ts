import supertest from 'supertest';
import nock from 'nock';

import app from '../../../../../src/app';
import raiseMocks from '../../../helpers/raise-mocks';
import Mock from '../../../../types/mock';
import getShopsByDomain from '../../../../../src/helper/get-shops-by-domain';

jest.mock('../../../../../src/helper/get-shops-by-domain');
const getShopsByDomainMock = getShopsByDomain as jest.MockedFunction<typeof getShopsByDomain>;

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const yabroFilterMock: Mock = require('../../__mocks__/yabro-filter');
const laasMock = require('../../__mocks__/laas.mock');
const categoriesMatchMock = require('./__mocks__/categories-match');
const multiMatchMock = require('./__mocks__/multi-match.model-match');
const modelMock = require('./__mocks__/model.model-match');
const modelOffersMock = require('./__mocks__/model-offers');
const modelOffersPremiumMock = require('./__mocks__/model-offers-premium');
const modelOpinionsMock = require('../__mocks__/model-opinions.mock');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.model-match');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: '33ca86c0a6805944f9cabe586c5e3190',
    v: '201907311213',
    isSelectorExists: true,
    transaction_id: 'jyzyrrls8doedn84nk4bg1rgsp4r9dn6',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: 1008,
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
        installId: 'ef3b0c45-754c-4b31-99c5-3670bb8382b0',
        installTime: 1565103567549,
    },
    adult: true,
    screen_size: {
        width: 2048,
        height: 928,
    },
    screen_resolution: {
        ratio: 1.25,
        width: 2048,
        height: 1071,
    },
    viewport: {
        width: 2031,
        height: 928,
    },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: false,
    url: 'https://www.avito.ru/sankt-peterburg/bytovaya_tehnika/hlebopech_panasonic_sd-2501_1786340164',
    pictures_by_tg: ['https://www.avito.ru/img/share/auto/5886174073', ''],
    h1_by_df: 'Хлебопечь Panasonic sd-2501',
    name_by_sl: 'Хлебопечь Panasonic sd-2501',
    category_by_sl:
        'Все объявления в Санкт-Петербурге/Для дома и дачи/Бытовая техника/Для кухни/Мелкая кухонная техника',
    price_by_sl: 7500,
    url_by_sh: 'https://www.avito.ru/sankt-peterburg/bytovaya_tehnika/hlebopech_panasonic_sd-2501_1786340164',
    name_by_og: 'Хлебопечь Panasonic sd-2501',
};

describe('Search pipeline: model-match', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            categoriesMatchMock,
            multiMatchMock,
            modelMock,
            modelOffersPremiumMock,
            modelOffersMock,
            modelOpinionsMock,
        );

        getShopsByDomainMock.mockImplementation(() => Promise.resolve(shops));
    });

    afterAll(() => {
        nock.cleanAll();
    });

    test('should find model', async () => {
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
