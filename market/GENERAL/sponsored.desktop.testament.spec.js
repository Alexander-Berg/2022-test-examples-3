// @flow

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

import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {
    PRODUCT,
    OFFER,
    OFFER_ID,
    PRODUCT_ID,
} from './__mock__/partnerInfoMock.js';

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

const WIDGET_PATH = require.resolve('@self/platform/widgets/content/KMDefaultOffer');

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

describe('Widget: KMDefaultOffer', () => {
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

    test(`Рекламная карточка модели. Ресурс getTopOffersForProduct вызывается с параметрами
        pp: SPONSORED_PRODUCT_DEFAULT_OFFER и offersSet: defaultList`, async () => {
        await makeContext({sponsored: '1'});
        await apiaryLayer.mountWidget(WIDGET_PATH);

        await jestLayer.backend.runCode(() => {
            const expect = require('expect');

            expect(resolveLegacyReportResourceSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    method: 'getTopOffersForProduct',
                    options: expect.objectContaining({
                        offersSet: 'defaultList',
                        pp: 'SPONSORED_PRODUCT_DEFAULT_OFFER',
                    }),
                }));
        }, []);
    });

    test(`Карточка модели. Ресурс getTopOffersForProduct вызывается с параметром
        pp: PRODUCT_BUNDLE_MAIN`, async () => {
        await makeContext();
        await apiaryLayer.mountWidget(WIDGET_PATH);

        await jestLayer.backend.runCode(() => {
            const expect = require('expect');

            expect(resolveLegacyReportResourceSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    method: 'getTopOffersForProduct',
                    options: expect.objectContaining({
                        pp: 'PRODUCT_BUNDLE_MAIN',
                    }),
                }));
        }, []);
    });
});
