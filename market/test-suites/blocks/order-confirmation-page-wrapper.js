import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {PAYMENT_METHODS} from '@self/root/src/entities/payment/constants';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';

import TrustFrame from '@self/root/src/spec/page-objects/TrustFrame';
import TrustExternalCardMobileForm from '@self/root/src/spec/page-objects/TrustExternalCardMobileForm';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import OrderConfirmationSubpageHeader from '@self/root/src/widgets/content/OrderConfirmationSubpageHeader/__pageObject';
import IFrameComponent from '@self/root/src/widgets/content/TrustFrame/components/IFrameComponent/__pageObject';

import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';

import unpaid from '@self/platform/spec/hermione/test-suites/blocks/orderConfirmation/__unpaid';
import common from '@self/platform/spec/hermione/test-suites/blocks/orderConfirmation/n-checkout-thankyou';
import {getCartList} from '@self/root/src/spec/hermione/scenarios/cartResource';


/**
 * Хэлпер для получения Promise с информацией о пользователе
 * @param testCase
 * @param [user] - предустановленная информация
 * @return {Promise}
 */
function prepareUserInfo(testCase, user) {
    if (user) {
        return Promise.resolve(user);
    }

    return testCase.browser.yaUserStub();
}

/**
 * Хэлпер для получения Promise c корзиной пользователя
 * @param testCase
 * @param [userPromise] - предустановленная информация. Если list не указан, обязателен
 * @param [list] - предустановленная информация о корзине
 * @return {Promise}
 */
function prepareCartListPromise(testCase, userPromise, list) {
    if (list) {
        return Promise.resolve(list);
    }

    return userPromise.then(userData => testCase.browser.yaScenario(testCase, getCartList, userData));
}

const cleanCartScenario = {
    name: 'Очищаем корзину пользователя',
    /**
     * @param {Object} [user] - становится обязательным, если используется не в контексте страницы
     * @param {string} [user.uid]
     * @param {string} [user.yandexuid]
     * @param {Object} [list] - подготовленная информация о корзине
     * @param {number[]} [list.ids]
     * @param {number} [list.listId]
     * @return {Promise}
     */
    func({user, list} = {}) {
        if (this.getMeta('environment') === 'kadavr') {
            return this.browser.setState('Carter.items', []);
        }

        const userDataPromise = prepareUserInfo(this, user);
        const cartListPromise = prepareCartListPromise(this, userDataPromise, list);

        return Promise
            .all([userDataPromise, cartListPromise])
            .then(([{uid, yandexuid}, cartList]) => {
                const cartItemIds = _.map(cartList.result.items, 'id');
                const listId = cartList.result.id;

                if (!_.size(cartItemIds) || !listId || listId === -1) {
                    return this.browser.allure.runStep('Корзина уже пуста - очистки не происходит.', () => {});
                }

                return this.browser.yaResource('carter.removeItemsFromCart', {
                    uid,
                    yandexuid,
                    cartItemIds,
                    listId,
                });
            });
    },
};

export default makeSuite('"Спасибо за заказ".', {
    defaultParams: {
        items: [{
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
        }],
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                trustFrameComponent: () => this.createPageObject(IFrameComponent),
                trustFrame: () => this.createPageObject(TrustFrame),
                trustExternalCardFormFrame: () => this.createPageObject(TrustExternalCardMobileForm),
                orderConfirmation: () => this.createPageObject(OrderConfirmation),
                orderConfirmationSubpageHeader: () => this.createPageObject(OrderConfirmationSubpageHeader),
            });
        },

        'Предоплатный неоплаченный заказ.': mergeSuites(
            {
                beforeEach() {
                    return prepareUnpaidOrderConfirmationPage.call(this, {
                        region: this.params.region,
                        items: this.params.items,
                    });
                },
            },

            prepareSuite(unpaid, {})
        ),

        'Постоплатный заказ.': mergeSuites(
            {
                beforeEach() {
                    return preparePostpaidOrderConfirmationPage.call(this, {
                        region: this.params.region,
                        items: this.params.items,
                    });
                },
            },

            prepareSuite(common, {})
        ),
    },
});

function prepareUnpaidOrderConfirmationPage({region, items}) {
    return this.browser.yaScenario(this, prepareThankPage, {
        orders: [{
            items,
            deliveryType: DELIVERY_TYPES.DELIVERY,
        }],
        region,
        paymentOptions: {
            paymentType: PAYMENT_TYPE.PREPAID,
            paymentMethod: PAYMENT_METHODS.YANDEX,
            status: ORDER_STATUS.UNPAID,
        },
    });
}

function preparePostpaidOrderConfirmationPage({region, items}) {
    const {browser} = this;

    return browser.yaScenario(this, prepareOrder, {
        region,
        orders: [{
            items,
            deliveryType: DELIVERY_TYPES.DELIVERY,
        }],
        paymentType: PAYMENT_TYPE.POSTPAID,
        paymentMethod: PAYMENT_METHODS.CASH_ON_DELIVERY,
    })
        .then(result => browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS_CONFIRMATION, {
            orderId: _.get(result, 'orders[0].id'),
        }))
        // Если в корзине останутся товары, то вид спасибо может отличаться
        .then(() => browser.yaScenario(this, cleanCartScenario))
        .then(() => browser.yaPageReloadExtended())
        .then(() => browser.yaDisableCSSAnimation());
}
