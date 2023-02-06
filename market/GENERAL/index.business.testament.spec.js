// flow

import {makeMirror} from '@self/platform/helpers/testament';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {createOfferForProduct, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// price-helpers
import {getCurrency, pricesValue} from '@self/root/src/utils/price';
import {NON_BREAKING_SPACE_CHAR as NOWRAP_SPACE} from '@self/root/src/constants/string';

// fixtures
import productKettle from '@self/root/src/spec/hermione/kadavr-mock/report/product/kettle';
import offerKettle from '@self/root/src/spec/hermione/kadavr-mock/report/offer/kettle';

// page-object
import SearchSnippetOfferCard from '@self/project/src/components/Search/Snippet/Offer/Card/__pageObject';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext(cookies = {}) {
    const cookie = {
        kadavr_session_id: await kadavrLayer.getSessionId(),
        ...cookies,
    };

    await mandrelLayer.initContext({
        route: {
            name: 'market:list',
            data: {},
        },
        request: {
            cookie,
        },
    });
}

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
    apiaryLayer = mirror.getLayer('apiary');
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    kadavrLayer = mirror.getLayer('kadavr');

    await jestLayer.doMock(
        require.resolve('@self/root/src/entities/platform/utils/getPlatform'),
        () => ({
            getPlatform: () => 'desktop',
        })
    );

    await jestLayer.doMock(
        require.resolve('@self/root/src/widgets/content/m2b/M2BNoticeProductSearch'),
        () => ({
            create: () => Promise.resolve(null),
        })
    );

    await jestLayer.doMock(
        require.resolve('@self/root/src/resolvers/search/resolveSearchVirtualizationSync'),
        () => ({
            resolveSearchVirtualizationSync: () => false,
        })
    );
});

afterAll(() => {
    mirror.destroy();
});

describe('Widget: Serp', () => {
    const WIDGET_PATH = '@self/root/src/widgets/content/search/Serp';
    describe('Пользователь m2b', () => {
        describe('Сниппет продукта', () => {
            let offerKettleWithoutVAT;
            beforeEach(async () => {
                offerKettleWithoutVAT = {
                    ...offerKettle,
                    prices: {
                        ...offerKettle.prices,
                        valueWithoutVAT: '15.15',
                    },
                };
                const reportState = mergeState([
                    createProduct(productKettle, productKettle.id),
                    createOfferForProduct(offerKettleWithoutVAT, productKettle.id, offerKettle.wareId),
                    {
                        data: {
                            search: {
                                total: 1,
                            },
                        },
                    },
                ]);

                await kadavrLayer.setState(
                    'report',
                    reportState
                );

                await makeContext({m2b: '1'});
            });

            it('должен содержать цену без НДС', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {props: {withIncuts: false}});

                const price = container.querySelector(SearchSnippetOfferCard.price);
                const expectedPrice = `${pricesValue(offerKettleWithoutVAT.prices.valueWithoutVAT)}${NOWRAP_SPACE}${getCurrency(offerKettle.prices.currency)}${NOWRAP_SPACE}без НДС`;

                expect(price).toBeTruthy();
                expect(price.textContent).toBe(expectedPrice);
            });
        });
    });
});
