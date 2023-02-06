import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {createActualizedCartResult, prepareCartItems, DEFAULT_ITEM} from '@yandex-market/kadavr/mocks/Eats/helpers';

// mocks
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

// suites
import headerSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/header';
import summarySuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/summary';
import cartListSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/cartList';
import changeCountOneOffer from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/changeCountOneOffer';

// pageObjects
import SoldOutOverlay
    from '@self/root/src/widgets/content/cart/CartList/containers/CartOfferPictureContainer/__pageObject';

const SHOP_ID = express.offerExpressMock.shop.id;
const SHOP_NAME = express.offerExpressMock.shop.name;
const SHOP_SLUG = express.offerExpressMock.shop.slug;
const BUSINESS_ID = express.offerExpressMock.shop.business_id;

module.exports = makeSuite('Для retail офера. Оффер доступен в репорте', {
    environment: 'kadavr',

    story: mergeSuites(
        prepareSuite(headerSuite, {
            params: {
                availableItemsCount: 1,
                unavailableTotalCount: 0,
                cartTitle: SHOP_NAME,
                businessId: BUSINESS_ID,
                businessSlug: SHOP_SLUG,
                thresholds: 'Ещё 950₽ до бесплатной доставки',
            },
            hooks: {
                async beforeEach() {
                    await preparePage.call(this);
                },
            },
        }),

        prepareSuite(summarySuite, {
            params: {
                shouldShowSummary: true,
                expectedPrice: DEFAULT_ITEM.price,
            },
            hooks: {
                async beforeEach() {
                    await preparePage.call(this);
                },
            },
        }),

        prepareSuite(cartListSuite, {
            params: {
                shouldShowBadge: false,
            },
            hooks: {
                async beforeEach() {
                    await preparePage.call(this);

                    this.setPageObjects({
                        soldOutOverlay: () => this.createPageObject(SoldOutOverlay),
                    });
                },
            },
        }),

        prepareSuite(changeCountOneOffer, {
            params: {
                price: DEFAULT_ITEM.price,
            },
            hooks: {
                async beforeEach() {
                    await preparePage.call(this, 3, 2);
                },
            },
        }),

        {
            'Обнуление стока.': mergeSuites(
                prepareSuite(summarySuite, {
                    params: {
                        shouldShowSummary: false,
                    },
                    hooks: {
                        async beforeEach() {
                            await preparePage.call(this, 3, 0);
                        },
                    },
                }),

                prepareSuite(cartListSuite, {
                    params: {
                        shouldShowBadge: true,
                    },
                    hooks: {
                        async beforeEach() {
                            await preparePage.call(this, 3, 0);

                            this.setPageObjects({
                                soldOutOverlay: () => this.createPageObject(SoldOutOverlay),
                            });
                        },
                    },
                })
            ),
        }
    ),
});


async function preparePage(itemCount, availableCount) {
    const dsbsCarts = [
        buildCheckouterBucket({
            shopId: SHOP_ID,
            items: [{
                skuMock: express.skuExpressMock,
                offerMock: {...express.offerExpressMock, prices: {value: DEFAULT_ITEM.price}, foodtechType: 'retail'},
                count: itemCount != null ? itemCount : 1,
                features: ['eats_retail'],
            }],
            deliveryOptions: [{
                ...deliveryDeliveryMock,
                deliveryPartnerType: 'SHOP',
            }],
        }),
    ];

    const items = prepareCartItems({item: {
        count: availableCount != null ? availableCount : 1,
        feed_offer_id: express.offerExpressMock.shop.feed.offerId,
    }});

    const actualizedCartResult = createActualizedCartResult({
        data: {
            items,
            shipping_cost: 100,
        },
        shopId: SHOP_ID,
    });

    await this.browser.yaScenario(
        this,
        prepareMultiCartState,
        dsbsCarts
    );

    await this.browser.setState('Eats.data.fetchActualizedCart', actualizedCartResult);
    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
    await this.browser.yaScenario(this, waitForCartActualization);
}
