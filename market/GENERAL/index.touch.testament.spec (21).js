// @flow
// flowlint-next-line untyped-import: off
import {createOfferForProduct, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {getCurrency} from '@self/root/src/utils/price';

// page-objects
// flowlint-next-line untyped-import: off
import SearchProductTile from '@self/project/src/components/SearchProductTile/__PageObject';
// flowlint-next-line untyped-import: off
import CompareTumbler from '@self/project/src/components/CompareTumbler/__PageObject';
// flowlint-next-line untyped-import: off
import DealsTerms from '@self/project/src/components/DealsTerms/__pageObject';
// flowlint-next-line untyped-import: off
import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
// flowlint-next-line untyped-import: off
import WishlistToggler from '@self/root/src/components/WishlistToggler/__pageObject';

// fixtures
// flowlint-next-line untyped-import: off
import productKettle from '@self/root/src/spec/hermione/kadavr-mock/report/product/kettle';
// flowlint-next-line untyped-import: off
import offerKettle from '@self/root/src/spec/hermione/kadavr-mock/report/offer/kettle';

// suites
import {createEmptyStateSuite} from './emptyState';

import {ORDER, ORDER_ID} from './mock';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

const WIDGET_PATH = '@self/project/src/widgets/content/PurchasedGoods';

async function makeContext(user = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    await mandrelLayer.initContext({
        request: {
            cookie,
        },
        user: {
            ...user,
        },
    });
}

beforeAll(async () => {
    mockIntersectionObserver();
    mirror = await makeMirrorTouch({
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

    await jestLayer.runCode(() => {
        const {mockRouter} = require('@self/project/src/helpers/testament/mock');
        mockRouter({
            'external:yandex-passport': () => '//pass-test.yandex.ru',
            'market:index': '//market.yandex.ru',
        });
    }, []);
});

afterAll(() => {
    mirror.destroy();
});

// eslint-disable-next-line jest/no-disabled-tests
describe('Widget: PurchasedGoods', () => {
    createEmptyStateSuite(makeContext, () => mirror);

    describe('Список ранее купленных товаров', () => {
        describe('Сниппет продукта', () => {
            beforeEach(async () => {
                await kadavrLayer.setState(
                    'Checkouter',
                    {
                        collections: {
                            order: {
                                [ORDER_ID]: ORDER,
                            },
                        },
                    }
                );

                await kadavrLayer.setState(
                    'report',
                    getReportState()
                );

                await makeContext({isAuth: true});
            });

            it('должен содержать кпноку добавления в избранное', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const wishlistToggler = container.querySelector(WishlistToggler.root);

                expect(wishlistToggler).toBeTruthy();
            });

            it('должен содержать кпноку добавления в сравнение', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const compareTumbler = container.querySelector(`${SearchProductTile.root} ${CompareTumbler.root}`);

                expect(compareTumbler).toBeTruthy();
            });

            it('должен содержать цену', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const price = container.querySelector(SearchProductTile.price);

                expect(price).toBeTruthy();
                expect(price.textContent).toBe(`${offerKettle.prices.value} ${getCurrency(offerKettle.prices.currency)}`);
            });

            it('должен содержать информацию о кешбеке', async () => {
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
                const cashbackInfo = container.querySelector(`${DealsTerms.root} ${CashbackInfo.root}`);

                expect(cashbackInfo).toBeTruthy();
            });
        });
    });
});

function getReportState() {
    return mergeState([
        createProduct(productKettle, productKettle.id),
        createOfferForProduct({
            ...offerKettle,
            promos: [
                {
                    type: 'blue-cashback',
                    key: 'JwguUZO8-HIaOJ1J4_k0_Q',
                    description: 'Кэшбэк на все',
                    shopPromoId: '3vDxyRpFM8ycDVJiunVGRA',
                    startDate: '2020-09-14T21:00:00Z',
                    endDate: '2024-12-30T21:00:00Z',
                    share: 0.05,
                    version: 1,
                    priority: 199,
                    value: 207,
                },
            ],
        }, productKettle.id, offerKettle.wareId),
    ]);
}
