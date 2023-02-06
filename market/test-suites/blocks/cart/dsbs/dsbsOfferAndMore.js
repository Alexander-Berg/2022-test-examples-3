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

// pageObjects
import CartTotalInformation
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartParcel from '@self/root/src/widgets/content/cart/CartList/components/CartParcel/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';

// mocks
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as fulfilmentKettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as dropshipPhone from '@self/root/src/spec/hermione/kadavr-mock/report/dropship';
import * as crossdockKovrik from '@self/root/src/spec/hermione/kadavr-mock/report/crossdock';

// suites
import mainInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/mainInfo';
import cartItemInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartItemInfo';
import totalInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/totalInfo';
import cartParcelSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/page/cartParcelInfo';

import params from './params';


const cart = buildCheckouterBucket({
    items: [{
        skuMock: dsbs.skuPhoneMock,
        offerMock: dsbs.offerPhoneMock,
        count: 1,
    }, {
        skuMock: fulfilmentKettle.skuMock,
        offerMock: fulfilmentKettle.offerMock,
        count: 1,
    }, {
        skuMock: dropshipPhone.skuPhoneMock,
        offerMock: dropshipPhone.offerPhoneMock,
        count: 1,
    }, {
        skuMock: crossdockKovrik.skuKovrikMock,
        offerMock: crossdockKovrik.offerKovrikMock,
        count: 1,
    }],
});

module.exports = makeSuite('Кроссдок + фулфилмент + dsbs', {
    environment: 'kadavr',
    id: 'bluemarket-3748',
    issue: 'MARKETFRONT-32914',

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartTotalInformation: () => this.createPageObject(CartTotalInformation),
                    cart: () => this.createPageObject(CartParcel),
                    removedCartItem: () => this.createPageObject(
                        CartItem,
                        {
                            parent: this.cartGroup,
                            root: `${BusinessGroupsStrategiesSelector.bucket(1)} ${CartItem.root}`,
                        }
                    ),
                });

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    [cart]
                );

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});

                await this.browser.yaScenario(this, waitForCartActualization);
            },
        },

        prepareSuite(mainInfoSuite, {
            params: {
                itemCount: 4,
            },
        }),

        prepareSuite(totalInfoSuite, {
            params: {
                totalPrice: Number(dsbs.offerPhoneMock.prices.value) +
                    Number(fulfilmentKettle.offerMock.prices.value) +
                    Number(dropshipPhone.offerPhoneMock.prices.value) +
                    Number(crossdockKovrik.offerKovrikMock.prices.value),
            },
        }),

        prepareSuite(cartParcelSuite, {
            params: {
                date: 'c 23 февраля',
                supplier: 'продавца',
                count: 4,
            },
        }),

        prepareSuite(cartItemInfoSuite, {
            suiteName: 'Информация о товаре. DSBS офер.',
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: params.hasWishlistBtn,
            },
            pageObjects: {
                cartItem() {
                    return this.createPageObject(CartItem, {
                        root: `${CartItemGroup.root}:nth-child(1) ${CartItem.root}`,
                        parent: this.cart,
                    });
                },
            },
        }),

        prepareSuite(cartItemInfoSuite, {
            suiteName: 'Информация о товаре. Фулфилмент офер.',
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: true,
            },
            pageObjects: {
                cartItem() {
                    return this.createPageObject(CartItem, {
                        root: `${CartItemGroup.root}:nth-child(2) ${CartItem.root}`,
                        parent: this.cart,
                    });
                },
            },
        }),

        prepareSuite(cartItemInfoSuite, {
            suiteName: 'Информация о товаре. Дропшип офер.',
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: true,
            },
            pageObjects: {
                cartItem() {
                    return this.createPageObject(CartItem, {
                        root: `${CartItemGroup.root}:nth-child(3) ${CartItem.root}`,
                        parent: this.cart,
                    });
                },
            },
        }),

        prepareSuite(cartItemInfoSuite, {
            suiteName: 'Информация о товаре. Кросдок офер.',
            params: {
                hasRemoveBtn: true,
                hasWishlistBtn: true,
            },
            pageObjects: {
                cartItem() {
                    return this.createPageObject(CartItem, {
                        root: `${CartItemGroup.root}:nth-child(4) ${CartItem.root}`,
                        parent: this.cart,
                    });
                },
            },
        })
    ),
});
