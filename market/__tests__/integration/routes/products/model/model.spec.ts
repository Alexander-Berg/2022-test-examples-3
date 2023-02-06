import supertest from 'supertest';
import nock from 'nock';

import app from '../../../../../src/app';
import raiseMocks from '../../../helpers/raise-mocks';
import Mock from '../../../../types/mock';
import * as tvm from '../../../../../src/API/yandex/tvm';
import getShopsByDomain from '../../../../../src/helper/get-shops-by-domain';

jest.mock('../../../../../src/API/yandex/tvm');

interface TvmMock {
    ticket: () => Promise<string>;
}
const tvmMock = tvm as jest.Mocked<TvmMock>;
tvmMock.ticket.mockImplementation(() => new Promise((resolve) => resolve('ticket')));

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
const modelMock = require('./__mocks__/model.model');
const modelOffersPremiumMock = require('./__mocks__/model-offers-premium.model');
const modelOffersMock = require('./__mocks__/model-offers.model');
const modelOpinionsMock = require('../__mocks__/model-opinions.mock');
const socialProfileMock = require('../__mocks__/social-profile.mock');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.model');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: '70ac3e5878e7b3a8410a574221766c1d',
    v: '201910231216',
    isSelectorExists: true,
    transaction_id: 'k285hi9jielaqf0mycihu6t0q7nd4lf2',
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
    group: 'A',
    adult: true,
    screen_size: { width: 1317, height: 876 },
    screen_resolution: { ratio: 1, width: 1920, height: 1019 },
    viewport: { width: 1300, height: 9562 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: true,
    url: 'https://beru.ru/product/yandex-stantsiia-umnaia-kolonka-dlia-umnogo-doma/100307940934',
    pictures_by_tg: ['//avatars.mds.yandex.net/get-mpic/1056698/img_id4639436396847366464.png/orig'],
    h1_by_df: 'Яндекс.Станция',
    price_by_md: 10990,
    name_by_sl: 'Яндекс.Станция',
    pictures_by_sl: ['//avatars.mds.yandex.net/get-mpic/1056698/img_id4639436396847366464.png/orig'],
    category_by_sl: 'Устройства для Умного дома/Яндекс',
    price_by_sl: 10990,
    url_by_sh: 'https://beru.ru/product/yandex-stantsiia-umnaia-kolonka-dlia-umnogo-doma/100307940934',
    name_by_og: 'Яндекс.Станция',
    self_by_sd: false,
};

describe('Search pipeline: model', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            modelMock,
            modelOffersPremiumMock,
            modelOffersMock,
            modelOffersMock,
            modelOffersMock,
            modelOpinionsMock,
            socialProfileMock,
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
