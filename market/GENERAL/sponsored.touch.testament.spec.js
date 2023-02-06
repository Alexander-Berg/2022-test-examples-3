// @flow

import {PackedFunction} from '@yandex-market/testament/mirror';

import {makeMirror} from '@self/platform/helpers/testament';

import {
    // flowlint-next-line untyped-import: off
    createProduct,
    // flowlint-next-line untyped-import: off
    createShopInfo,
    // flowlint-next-line untyped-import: off
    mergeState,
    // flowlint-next-line untyped-import: off
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
// flowlint-next-line untyped-import: off
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';


import {
    OFFER,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
    PRODUCT_PROMISE_MOCK,
    DEFAULT_OFFER_PROMISE_MOCK,
    PAGE_STATE_PROMISE_MOCK,
} from './__mock__/promocodeMock.js';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

const WIDGET_PATH = require.resolve('@self/platform/widgets/parts/DefaultOffer');
const WIDGET_OPTIONS = new PackedFunction(
    (productMock, defaultOfferMock, pageStateMock) => ({
        productPromise: Promise.resolve(productMock),
        defaultOfferPromise: Promise.resolve(defaultOfferMock),
        pageStatePromise: Promise.resolve(pageStateMock),
        isDynamic: true,
    }),
    [PRODUCT_PROMISE_MOCK, DEFAULT_OFFER_PROMISE_MOCK, PAGE_STATE_PROMISE_MOCK]
);

async function makeContext(params = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        request: {
            cookie,
            params: {
                productId: PRODUCT_ID,
                ...params,
            },
        },
    });
}

const setReportState = async () => {
    // $FlowFixMe не видит вложенные сущности
    const offer = createOffer(OFFER);

    const reportState = mergeState([
        createShopInfo(),
        createProduct(PRODUCT, PRODUCT_ID),
        createOfferForProduct(offer, PRODUCT_ID, OFFER_ID),
        {
            data: {
                search: {
                    total: 1,
                },
            },
        },
    ]);

    await kadavrLayer.setState('report', reportState);
};

beforeAll(async () => {
    mockIntersectionObserver();

    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });
    mandrelLayer = mirror.getLayer('mandrel');
    kadavrLayer = mirror.getLayer('kadavr');
    apiaryLayer = mirror.getLayer('apiary');
    jestLayer = mirror.getLayer('jest');
});

afterAll(() => {
    mirror.destroy();
    jest.useRealTimers();
});

describe('Widget: DefaultOffer', () => {
    let resolveLegacyReportResourceSpy;
    beforeAll(async () => {
        mockIntersectionObserver();
        await setReportState();

        await jestLayer.backend.runCode(() => {
            resolveLegacyReportResourceSpy = jest.spyOn(
                require('@self/project/src/resolvers/search/resolveLegacyReportResource'),
                'resolveLegacyReportResource'
            );
        }, []);
    });

    test(`Рекламная карточка мдели. Ресурс getProductOffers вызывается с параметрами
    isSponsored: true и offers-set: defaultList`, async () => {
        await makeContext({sponsored: '1'});
        await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

        await jestLayer.backend.runCode(() => {
            const expect = require('expect');

            expect(resolveLegacyReportResourceSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    method: 'getProductOffers',
                    options: expect.objectContaining({
                        'offers-set': 'defaultList',
                        isSponsored: true,
                    }),
                }));
        }, []);
    });

    test('Рекламная карточка мдели. Ресурс getProductOffers вызывается без параметра isSponsored', async () => {
        await makeContext();
        await apiaryLayer.mountWidget(WIDGET_PATH, WIDGET_OPTIONS);

        await jestLayer.backend.runCode(() => {
            const expect = require('expect');

            expect(resolveLegacyReportResourceSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    method: 'getProductOffers',
                    options: expect.not.objectContaining({
                        isSponsored: true,
                    }),
                }));
        }, []);
    });
});
