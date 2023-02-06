import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';

import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartOrderInfo
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartLayout from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartEmpty from '@self/root/src/widgets/content/cart/CartEmpty/components/View/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import PromocodeComponent from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import cartDataLayer from '@self/root/src/spec/hermione/test-suites/blocks/ecommerce/nonChangedCart';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import degradationCart from '@self/platform/spec/hermione/test-suites/blocks/cart/degradation';
import toCheckout from '@self/platform/spec/hermione/test-suites/blocks/cart/toCheckout';
import redirectToCartFromHeader from '@self/platform/spec/hermione/test-suites/blocks/cart/redirectToCartFromHeader';
import dropshipSuite from '@self/platform/spec/hermione/test-suites/blocks/cart/dropship';
import dropshipInMulticartSuite from '@self/platform/spec/hermione/test-suites/blocks/cart/dropshipInMultiCart';
import discount from '@self/platform/spec/hermione/test-suites/blocks/cart/discount';
import promocode from '@self/platform/spec/hermione/test-suites/blocks/cart/promocode';
import widgetsMetrikaSuite from '@self/root/src/spec/hermione/test-suites/touch.blocks/widgetsMetrika/cartPage';
import weight from '@self/platform/spec/hermione/test-suites/blocks/cart/weight';
import coinCheck from '@self/platform/spec/hermione/test-suites/blocks/cart/coinCheck';
import credit from '@self/platform/spec/hermione/test-suites/blocks/cart/credit';
import cheapestAsGift from '@self/platform/spec/hermione/test-suites/blocks/cart/cheapestAsGift';
import genericBundle from '@self/root/src/spec/hermione/test-suites/blocks/cart/genericBundle';
import blueFlash from '@self/root/src/spec/hermione/test-suites/blocks/cart/blueFlash';
import secretSale from '@self/root/src/spec/hermione/test-suites/blocks/cart/secretSale';
import notInStock from '@self/platform/spec/hermione/test-suites/blocks/cart/notInStock';
import promocodePromo from '@self/platform/spec/hermione/test-suites/blocks/cart/promocodePromo';
import cartBonuses from '@self/root/src/spec/hermione/test-suites/blocks/cart/bonus';
import dsbsOffer from '@self/root/src/spec/hermione/test-suites/blocks/cart/dsbs/dsbsOffer';
import dsbsOfferAndMore from '@self/root/src/spec/hermione/test-suites/blocks/cart/dsbs/dsbsOfferAndMore';
import jewelrySuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/jewelry';
import welcomeCashbackbanner from '@self/root/src/spec/hermione/test-suites/blocks/cart/welcomeCashbackSuites/touchWelcomeCashbackBanner';
import growingCashbackPromoBanner from '@self/root/src/spec/hermione/test-suites/blocks/cart/growingCashbackPromoBanner';
import cartItem from '@self/platform/spec/hermione/test-suites/blocks/cartItem';
import cartPageLayout from '@self/platform/spec/hermione/test-suites/blocks/cartPageLayout';
import cartGroup from '@self/platform/spec/hermione/test-suites/blocks/cartGroup';
import bonusErrors from '@self/platform/spec/hermione/test-suites/blocks/cart/bonusErrors';
import largeCargoType from '@self/platform/spec/hermione/test-suites/blocks/cart/largeCargoType';
import summary from '@self/platform/spec/hermione/test-suites/blocks/cart/summary';
import accessories from '@self/platform/spec/hermione/test-suites/blocks/cart/accessories';
import cartCashback from '@self/root/src/spec/hermione/test-suites/blocks/cart/cashback';
import express from '@self/root/src/spec/hermione/test-suites/blocks/cart/express';
import treshold from '@self/root/src/spec/hermione/test-suites/blocks/cart/treshold';
import partialPurchase from '@self/platform/spec/hermione/test-suites/blocks/cart/partialPurchase';
import grouping from '@self/root/src/spec/hermione/test-suites/blocks/cart/grouping';
import pharma from '@self/root/src/spec/hermione/test-suites/blocks/cart/pharma';
import availableRetail from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/availableOffer';
import unavailableRetail from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/unavailableOffer';
import manyOffersRetail from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/manyOffers';
/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @start
 */
import cashbackForAllIncut from '@self/root/src/spec/hermione/test-suites/touch.blocks/cashbackForAll/incutInCart';
/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @end
 */
import uniqueOfferSuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/uniqueOffer';
import productsGrouping from '@self/root/src/spec/hermione/test-suites/blocks/cart/productsGrouping';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Корзина', {
    environment: 'testing',
    params: {
        ...commonParams.description,
        items: 'Товары в корзине',
    },
    defaultParams: {
        ...commonParams.value,
        count: 1,
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cartGroup: () => this.createPageObject(CartLayout),
                    orderInfo: () => this.createPageObject(CartOrderInfo, {parent: this.cartGroup}),
                    orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    preloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    orderTotal: () => this.createPageObject(OrderTotal),
                    promocodeWrapper: () => this.createPageObject(PromocodeComponent, {parent: this.orderInfo}),
                    promocodeInput: () => this.createPageObject(SubmitField, {parent: this.promocodeWrapper}),
                });
            },
        },

        prepareSuite(cartItem, {
            pageObjects: {
                cartItem() {
                    return this.createPageObject(CartItem, {root: `${CartItem.root}:nth-child(1)`});
                },
            },
        }),

        prepareSuite(cartPageLayout, {
            pageObjects: {
                cartPageLayout() {
                    return this.createPageObject(CartLayout);
                },
                cartEmpty() {
                    return this.createPageObject(CartEmpty, {parent: this.cartPageLayout});
                },
            },
        }),

        prepareSuite(cartGroup, {
            pageObjects: {
                cartGroup() {
                    return this.createPageObject(CartLayout);
                },
            },
        }),

        prepareSuite(discount, {
            suiteName: 'Неавторизованный пользователь. Оффер.',
        }),
        prepareSuite(discount, {
            suiteName: 'Авторизованный пользователь. Оффер.',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(promocode, {
            suiteName: 'Авторизованный пользователь. Промокоды.',
            params: {
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(promocode, {
            suiteName: 'Неавторизованный пользователь. Промокоды.',
        }),

        prepareSuite(toCheckout, {
            suiteName: 'Корзина. Переход в чекаут.',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(redirectToCartFromHeader, {
            suiteName: 'Корзина. Переход из шапки.',
        }),

        prepareSuite(dropshipSuite, {}),
        prepareSuite(dropshipInMulticartSuite, {}),
        prepareSuite(cartBonuses, {}),
        prepareSuite(bonusErrors, {}),
        prepareSuite(largeCargoType, {}),
        prepareSuite(redirectToCartFromHeader, {}),
        prepareSuite(summary, {}),
        prepareSuite(accessories, {}),
        prepareSuite(coinCheck, {}),
        credit,
        widgetsMetrikaSuite,
        prepareSuite(degradationCart, {
            suiteName: 'Неавторизованный пользователь. Деградация.',
        }),
        prepareSuite(degradationCart, {
            suiteName: 'Авторизованный пользователь. Деградация.',
            params: {
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(weight, {}),
        prepareSuite(cartDataLayer, {}),
        prepareSuite(cheapestAsGift, {}),
        prepareSuite(genericBundle, {}),
        prepareSuite(blueFlash, {}),
        prepareSuite(secretSale, {
            params: {
                pageId: PAGE_IDS_COMMON.CART,
            },
        }),
        prepareSuite(notInStock, {}),
        prepareSuite(promocodePromo, {}),
        prepareSuite(dsbsOffer),
        prepareSuite(dsbsOfferAndMore),
        prepareSuite(jewelrySuite),
        prepareSuite(welcomeCashbackbanner, {}),
        prepareSuite(cartCashback, {}),
        prepareSuite(express),
        prepareSuite(treshold),
        prepareSuite(partialPurchase, {}),
        prepareSuite(grouping, {}),
        prepareSuite(pharma, {}),
        prepareSuite(availableRetail),
        prepareSuite(unavailableRetail),
        prepareSuite(manyOffersRetail),
        growingCashbackPromoBanner,
        prepareSuite(uniqueOfferSuite),
        /**
         * @expFlag all_plus-4-all
         * @ticket MARKETPROJECT-8396
         * @start
         */
        cashbackForAllIncut,
        /**
         * @expFlag all_plus-4-all
         * @ticket MARKETPROJECT-8396
         * @end
         */
        prepareSuite(productsGrouping, {})
    ),
});
