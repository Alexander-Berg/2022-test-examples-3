import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// pageObject
import SoldOutOverlay from '@self/root/src/widgets/content/cart/CartList/containers/CartOfferPictureContainer/__pageObject';
import ExpiredCartOffer from '@self/root/src/widgets/content/cart/CartList/components/ExpiredCartOffer/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {createActualizedCartResult, prepareCartItems, DEFAULT_ITEM} from '@yandex-market/kadavr/mocks/Eats/helpers';

// mocks
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

// suites
import headerSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/header';
import cartListSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/cartList';
import summarySuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/summary';

const SHOP_ID = express.offerExpressMock.shop.id;
const SHOP_NAME = express.offerExpressMock.shop.name;
const SHOP_SLUG = express.offerExpressMock.shop.slug;
const BUSINESS_ID = express.offerExpressMock.shop.business_id;

/**
 * Проверяет список офферов в корзине
 */
module.exports = makeSuite('Два retail оффера от одного магазина, один недоступен', {
    environment: 'kadavr',

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    unavailableCartOffer: () => this.createPageObject(ExpiredCartOffer),
                    soldOutOverlay: () => this.createPageObject(SoldOutOverlay, {parent: this.unavailableCartOffer}),
                    selectAll: () => this.createPageObject(Checkbox, {
                        root: `${CartHeader.root} ${Checkbox.root}`,
                    }),
                });

                const carts = [
                    buildCheckouterBucket({
                        shopId: SHOP_ID,
                        items: [{
                            skuMock: express.skuExpressMock,
                            offerMock: {
                                ...express.offerExpressMock,
                                prices: {value: DEFAULT_ITEM.price},
                                foodtechType: 'retail',
                            },
                            count: 1,
                            features: ['eats_retail'],
                        },
                        {
                            skuMock: kettle.skuMock,
                            offerMock: {
                                ...kettle.offerKettleBnpl,
                                foodtechType: 'retail',
                                shop: express.offerExpressMock.shop,
                            },
                            count: 1,
                            features: ['eats_retail'],
                            isExpired: true,
                            isSkippedInReport: true,
                        }],
                        deliveryOptions: [{
                            ...deliveryDeliveryMock,
                            deliveryPartnerType: 'SHOP',
                        }],
                    }),
                ];

                const items = [
                    ...prepareCartItems({item: {
                        feed_offer_id: express.offerExpressMock.shop.feed.offerId,
                        count: 1,
                    }}),
                    ...prepareCartItems({item: {found: false, price: 0, discount_price: 0}}),
                ];

                const deliveryDetails = createActualizedCartResult({
                    data: {
                        items,
                        shipping_cost: 100,
                        left_for_free_delivery: 900,
                    },
                    shopId: SHOP_ID,
                });

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                await this.browser.setState('Eats.data.fetchActualizedCart', deliveryDetails);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});

                if (!await this.selectAll.isChecked()) {
                    await this.selectAll.toggle();
                }

                await this.browser.yaScenario(this, waitForCartActualization);
            },
        },

        prepareSuite(summarySuite, {
            params: {
                shouldShowSummary: true,
                expectedPrice: DEFAULT_ITEM.price,
            },
        }),

        prepareSuite(cartListSuite, {
            params: {
                shouldShowBadge: true,
            },
        }),

        prepareSuite(headerSuite, {
            params: {
                availableItemsCount: 1,
                unavailableItemsCount: 1,
                cartTitle: SHOP_NAME,
                businessId: BUSINESS_ID,
                businessSlug: SHOP_SLUG,
                thresholds: 'Ещё 950₽ до бесплатной доставки',
            },
        })
    ),
});
