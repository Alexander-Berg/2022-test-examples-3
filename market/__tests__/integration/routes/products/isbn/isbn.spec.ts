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
const categoriesMatchMock = require('./__mocks__/categories-match.isbn');
const categoriesSearchMock = require('./__mocks__/categories-search.isbn');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.isbn');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: 'b74954c68d3d8a9ab4a1ab4fcdc80a44',
    v: '201910231216',
    isSelectorExists: true,
    transaction_id: 'k2aspz1bm2euy98w53tmt94v1106ak7m',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1572252416355',
        installId: '0d6fbcdd-fe71-4a02-9d6a-075b149fda2a',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: { width: 1424, height: 622 },
    screen_resolution: '{"ratio":1,"width":1440,"height":787}',
    viewport: { width: 1407, height: 6262 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: false,
    url: 'https://www.labirint.ru/books/714296/',
    pictures_by_tg: ['https://img2.labirint.ru/books72/714296/big.jpg'],
    isbn_by_bk: '978-5-904662-34-9',
    h1_by_df: 'Гэри Джанни: Corpus Monstrum',
    price_by_df: 435,
    name_by_sl: 'Гэри Джанни: Corpus Monstrum',
    isbn_by_sl: '978-5-904662-34-9',
    pictures_by_sl: ['https://img2.labirint.ru/books72/714296/big.jpg'],
    category_by_sl: 'Комиксы, Манга, Артбуки/Комиксы',
    price_by_sl: 1268,
    currency_by_sl: 'RUB',
    vendor_by_sl: 'Zangavar',
    url_by_sh: 'https://www.labirint.ru/books/714296/',
    name_by_og: 'Corpus Monstrum',
    self_by_sd: false,
};

describe('Search pipeline: isbn', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(yabroFilterMock, laasMock, categoriesMatchMock, categoriesSearchMock);
        getShopsByDomainMock.mockImplementation(() => Promise.resolve(shops));
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
