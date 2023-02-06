import {
    prepareSuite,
    mergeSuites,
    makeSuite,
} from 'ginny';

import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import CartLayout from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartEmpty from '@self/root/src/widgets/content/cart/CartEmpty/components/View/__pageObject';
import CartTotalInformation from
    '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import PromocodeComponent from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import cartDataLayer from '@self/root/src/spec/hermione/test-suites/blocks/ecommerce/nonChangedCart';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import secretSale from '@self/root/src/spec/hermione/test-suites/blocks/cart/secretSale';
import degradationCart from '@self/platform/spec/hermione/test-suites/blocks/cart/degradation';
import credit from '@self/platform/spec/hermione/test-suites/blocks/cart/credit';
import discount from '@self/platform/spec/hermione/test-suites/blocks/cart/discount';
import widgetsMetrikaSuite from '@self/root/src/spec/hermione/test-suites/desktop.blocks/widgetsMetrika/cartPage';
import toCheckout from '@self/platform/spec/hermione/test-suites/blocks/cart/toCheckout';
import redirectToCartFromHeader from '@self/platform/spec/hermione/test-suites/blocks/cart/redirectToCartFromHeader';
import digitalSuite from '@self/platform/spec/hermione/test-suites/blocks/cart/digital';
import dropshipSuite from '@self/platform/spec/hermione/test-suites/blocks/cart/dropship';
import dropshipInMulticartSuite from '@self/platform/spec/hermione/test-suites/blocks/cart/dropshipInMultiCart';
import weight from '@self/platform/spec/hermione/test-suites/blocks/cart/weight';
import coinCheck from '@self/platform/spec/hermione/test-suites/blocks/cart/coinCheck';
import unpaidOrderWidget from '@self/root/src/spec/hermione/test-suites/desktop.blocks/unpaidOrder/orderWidget';
import cheapestAsGift from '@self/platform/spec/hermione/test-suites/blocks/cart/cheapestAsGift';
import genericBundle from '@self/root/src/spec/hermione/test-suites/blocks/cart/genericBundle';
import blueFlash from '@self/root/src/spec/hermione/test-suites/blocks/cart/blueFlash';
import notInStock from '@self/platform/spec/hermione/test-suites/blocks/cart/notInStock';
import promocode from '@self/platform/spec/hermione/test-suites/blocks/cart/promocode';
import cartBonuses from '@self/root/src/spec/hermione/test-suites/blocks/cart/bonus';
import cartCashback from '@self/root/src/spec/hermione/test-suites/blocks/cart/cashback';
import dsbsOffer from '@self/root/src/spec/hermione/test-suites/blocks/cart/dsbs/dsbsOffer';
import dsbsOfferAndMore from '@self/root/src/spec/hermione/test-suites/blocks/cart/dsbs/dsbsOfferAndMore';
import availableRetail from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/availableOffer';
import unavailableRetail from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/unavailableOffer';
import manyOffersRetail from '@self/root/src/spec/hermione/test-suites/blocks/cart/retail/manyOffers';
import jewelrySuite from '@self/root/src/spec/hermione/test-suites/blocks/cart/jewelry';
import treshold from '@self/root/src/spec/hermione/test-suites/blocks/cart/treshold';
import welcomeCashbackTooltip from '@self/root/src/spec/hermione/test-suites/blocks/cart/welcomeCashbackSuites/desktopWelcomeCashbackTooltip';
import express from '@self/root/src/spec/hermione/test-suites/blocks/cart/express';
import pharma from '@self/root/src/spec/hermione/test-suites/blocks/cart/pharma';
import partialPurchase from '@self/platform/spec/hermione/test-suites/blocks/cart/partialPurchase';
import grouping from '@self/root/src/spec/hermione/test-suites/blocks/cart/grouping';
import growingCashbackTooltip from '@self/root/src/spec/hermione/test-suites/desktop.blocks/growingCashback/tooltipSuites';
import agitation from '@self/platform/spec/hermione/test-suites/blocks/cart/agitation';
/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @start
 */
import cashbackForAllIncut from '@self/root/src/spec/hermione/test-suites/desktop.blocks/cashbackForAll/incutInCart';
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
                    orderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                    orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    promocodeWrapper: () => this.createPageObject(PromocodeComponent, {parent: this.orderInfo}),
                    promocodeInput: () => this.createPageObject(SubmitField, {parent: this.promocodeWrapper}),
                });
            },
        },

        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cartItem'), {
            pageObjects: {
                cartItem() {
                    return this.createPageObject(CartItem, {root: `${CartItem.root}:nth-child(1)`});
                },
            },
        }),

        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cartPageLayout'), {
            pageObjects: {
                cartPageLayout() {
                    return this.createPageObject(CartLayout);
                },
                cartEmpty() {
                    return this.createPageObject(CartEmpty, {parent: this.cartPageLayout});
                },
            },
        }),

        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cartGroup'), {
            pageObjects: {
                cartGroup() {
                    return this.createPageObject(CartLayout);
                },
            },
        }),

        prepareSuite(digitalSuite, {}),
        prepareSuite(dropshipSuite, {}),

        prepareSuite(dropshipInMulticartSuite, {}),

        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cart/largeCargoType'), {}),

        prepareSuite(cartBonuses, {}),
        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cart/degradationLoyalty'), {}),

        prepareSuite(coinCheck, {}),

        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cart/bonusErrors'), {}),

        prepareSuite(discount, {
            suiteName: 'Неавторизованный пользователь. Оффер.',
        }),
        prepareSuite(discount, {
            suiteName: 'Авторизованный пользователь. Оффер.',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(degradationCart, {
            suiteName: 'Неавторизованный пользователь. Деградация.',
        }),

        prepareSuite(degradationCart, {
            suiteName: 'Авторизованный пользователь. Деградация.',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(toCheckout, {
            suiteName: 'Корзина. Переход в чекаут.',
            params: {
                isAuthWithPlugin: true,
            },
        }),

        credit,

        prepareSuite(redirectToCartFromHeader, {
            suiteName: 'Корзина. Переход из шапки.',
        }),

        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cart/accessories'), {}),
        prepareSuite(require('@self/platform/spec/hermione/test-suites/blocks/cart/summary'), {}),

        widgetsMetrikaSuite,

        prepareSuite(weight, {}),

        prepareSuite(cartDataLayer, {}),
        prepareSuite(unpaidOrderWidget, {
            params: {
                pageId: PAGE_IDS_COMMON.CART,
            },
        }),
        prepareSuite(cheapestAsGift, {}),
        prepareSuite(genericBundle, {}),
        prepareSuite(blueFlash, {}),
        prepareSuite(secretSale, {
            params: {
                pageId: PAGE_IDS_COMMON.CART,
            },
        }),
        prepareSuite(notInStock, {}),
        prepareSuite(promocode, {}),
        prepareSuite(dsbsOffer),
        prepareSuite(dsbsOfferAndMore),
        prepareSuite(availableRetail),
        prepareSuite(unavailableRetail),
        prepareSuite(manyOffersRetail),
        prepareSuite(jewelrySuite),
        prepareSuite(cartCashback, {}),
        prepareSuite(welcomeCashbackTooltip, {}),
        prepareSuite(express, {}),
        prepareSuite(treshold),
        prepareSuite(partialPurchase, {}),
        prepareSuite(grouping, {}),
        prepareSuite(pharma, {}),
        growingCashbackTooltip,
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
        prepareSuite(productsGrouping, {}),
        prepareSuite(agitation, {})
    ),
});
