import {any} from 'ambar';

import {makeCase, makeSuite, mergeSuites} from 'ginny';

import * as tv from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {buildCheckouterBucket, buildCheckouterBucketLabel} from '@self/root/src/spec/utils/checkouter';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import AbstractOrderItem from '@self/root/src/components/TotalDeliveryInfo/Items/AbstractOrderItem/__pageObject';
import CartOrderInfo from
    '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';

export default makeSuite('Один оффер в корзине.', {
    environment: 'kadavr',
    id: 'bluemarket-2981',
    issue: 'BLUEMARKET-8189',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    orderInfoNotAvailable: () => this.createPageObject(AbstractOrderItem, {
                        root: `${AbstractOrderItem.root}${AbstractOrderItem.notAvailable}`,
                    }),
                    cartOrderInfo: () => this.createPageObject(CartOrderInfo),
                    checkoutButton: () => this.createPageObject(CheckoutButton),
                });
            },
        },

        makeSuite('Оффер кончилися по стокам.', {
            defaultParams: {
                items: [{
                    skuId: tv.skuMock.id,
                    offerId: tv.offerMock.wareId,
                    /**
                     * При загрузке страницы отрисуется состояние из картера - все в наличии
                     * После актуализации из чекаутера придет count = 0, появится надпись "Разобрали"
                     */
                    count: 0,
                }],
            },
            story: {
                async beforeEach() {
                    await prepareState.call(this, {
                        mocks: [tv],
                        withoutDelivery: true,
                    });

                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: this.params.region});
                    return this.browser.yaScenario(this, waitForCartActualization);
                },

                'Заголовок корзины содержит текст "Корзина"': makeCase({
                    test() {
                        return this.cartHeader.getTitleText()
                            .should.eventually.to.be.include(
                                CART_TITLE,
                                `Заголовок корзины должен содержать текст "${CART_TITLE}"`
                            );
                    },
                }),

                'Саммари корзины не отображается': makeCase({
                    test() {
                        return this.cartOrderInfo.isExisting()
                            .should.eventually.to.be.equal(
                                false,
                                'Саммари корзины не должно отображаться'
                            );
                    },
                }),
            },
        }),

        makeSuite('Оффера уже нет в репорте.', {
            defaultParams: {
                items: [{
                    skuId: tv.skuMock.id,
                    offerId: tv.offerMock.wareId,
                    /**
                     * При загрузке страницы отрисуется состояние из картера - все в наличии
                     * После актуализации из чекаутера придет count = 0, появится надпись "Разобрали"
                     */
                    count: 0,
                    isExpired: true,
                    isSkippedInReport: true,
                }],
            },
            story: {
                async beforeEach() {
                    await prepareState.call(this, {
                        mocks: [tv],
                    });

                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: this.params.region});
                },

                'Заголовок корзины содержит текст "Корзина"': makeCase({
                    test() {
                        return this.cartHeader.getTitleText()
                            .should.eventually.to.be.include(
                                CART_TITLE,
                                `Заголовок корзины должен содержать текст "${CART_TITLE}"`
                            );
                    },
                }),

                'Саммари корзины не отображается': makeCase({
                    test() {
                        return this.cartOrderInfo.isExisting()
                            .should.eventually.to.be.equal(
                                false,
                                'Саммари корзины не должно отображаться'
                            );
                    },
                }),
            },
        })
    ),
});

function prepareState({mocks = [], withoutDelivery}) {
    const carts = [];

    const cart = buildCheckouterBucket({
        label: buildCheckouterBucketLabel(
            this.params.items
                .filter(item => !item.isExpired)
                .map((item, i) => mocks[i].offerMock.wareId), 2),
        warehouseId: 2,
        items: this.params.items.map((item, i) => {
            if (item.isExpired) {
                return null;
            }

            return {
                ...item,
                skuMock: mocks[i].skuMock,
                offerMock: mocks[i].offerMock,
            };
        }).filter(Boolean),
        region: this.params.region,
        withoutDelivery,
    });

    if (cart.items.length) {
        carts.push(cart);
    }

    if (any(item => item.isExpired, this.params.items)) {
        const expipedCart = buildCheckouterBucket({
            shopId: 0,
            warehouseId: 0,
            items: this.params.items.map((item, i) => {
                if (!item.isExpired) {
                    return null;
                }

                return {
                    ...item,
                    skuMock: mocks[i].skuMock,
                    offerMock: mocks[i].offerMock,
                };
            }).filter(Boolean),
            region: this.params.region,
            withoutDelivery,
        });

        carts.push(expipedCart);
    }

    return this.browser.yaScenario(
        this,
        prepareMultiCartState,
        carts.filter(Boolean)
    );
}
