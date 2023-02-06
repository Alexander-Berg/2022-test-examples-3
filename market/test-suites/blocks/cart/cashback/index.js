import {makeCase, makeSuite} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {getBonusString, getFullBonusString} from '@self/root/src/utils/string';

import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import CartItemCashback
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferCashback/__pageObject';
import OrderTotal, {CashbackEmitTotal} from '@self/root/src/components/OrderTotalV2/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';

import {
    prepareCashbackProfile,
    prepareCashbackOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/cashback';
import {deliveryDeliveryMock, deliveryPickupMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

const CASHBACK_AMOUNT = 123;

const cart = buildCheckouterBucket({
    items: [{
        skuMock: kettle.skuMock,
        offerMock: {
            ...kettle.offerMock,
            promos: [
                {
                    type: 'blue-cashback',
                    value: CASHBACK_AMOUNT,
                },
            ],
        },
        count: 1,
    }],
    deliveryOptions: [
        deliveryDeliveryMock,
        deliveryPickupMock,
    ],
});

const cashbackOptionsProfiles = prepareCashbackProfile({
    cartId: cart.label,
    offerId: kettle.offerMock.feed.offerId,
    cashbackAmount: CASHBACK_AMOUNT,
    feedId: kettle.offerMock.feed.id,
});
const cashback = prepareCashbackOptions(CASHBACK_AMOUNT);

module.exports = makeSuite('Кэшбэк. Корзина.', {
    environment: 'kadavr',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
        expectedRowTitle: 'Заголовок строки о кешбеке в секции “Итого”',
        cashbackAmount: 'Размер кэшбэка',
    },
    defaultParams: {
        expectedRowTitle: 'Вернется на Плюс',
        isAuthWithPlugin: true,
        cashbackAmount: CASHBACK_AMOUNT,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartItem: () => this.createPageObject(
                    CartItem,
                    {root: `${CartItem.root}`}
                ),
                cartItemCashbackInfo: () => this.createPageObject(CartItemCashback, {
                    parent: this.cartItem,
                }),

                orderTotal: () => this.createPageObject(OrderTotal),
                cashbackEmitTotal: () => this.createPageObject(CashbackEmitTotal, {
                    parent: this.orderTotal,
                }),
                cashbackInfo: () => this.createPageObject(CashbackInfo, {
                    parent: this.cashbackEmitTotal,
                }),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [cart],
                {
                    additionalCheckouterCollections: {
                        cashbackOptionsProfiles,
                        cashback,
                    },
                }
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});

            return this.browser.yaScenario(this, waitForCartActualization);
        },
        'Бейдж на сниппете товара': makeCase({
            id: 'bluemarket-3632',
            async test() {
                await this.cartItem.isVisible()
                    .should.eventually.to.be.equal(true, 'Корзинный айтем должен быть виден');

                await this.cartItemCashbackInfo.isVisible()
                    .should.eventually.be.equal(true, 'Кэшбэк должен отображаться');

                return this.cartItemCashbackInfo.getCashbackText()
                    .should.eventually.to.be.equal(
                        `${this.params.cashbackAmount} ${getBonusString(this.params.cashbackAmount)}`,
                        `Текст бейджа кэшбэка должен быть “${getFullBonusString(this.params.cashbackAmount)}“`
                    );
            },
        }),

        'Инфо и бейдж в блоке “Итого“': makeCase({
            id: 'bluemarket-2480',
            async test() {
                await this.cashbackEmitTotal.isVisible()
                    .should.eventually.to.be.equal(true, 'Секция кешбека должна отображаться');

                await this.cashbackEmitTotal.getTitle()
                    .should.eventually.to.be.equal(
                        `${this.params.expectedRowTitle}`,
                        `Заголовок строки о кешбеке должен быть "${this.params.expectedRowTitle}"`
                    );

                return this.cashbackEmitTotal.getCashbackText()
                    .should.eventually.to.be.equal(
                        `${this.params.cashbackAmount}`,
                        `Количество баллов кешбека должно быть ${this.params.cashbackAmount}`
                    );
            },
        }),
    },
});
