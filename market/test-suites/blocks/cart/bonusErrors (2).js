import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import bonuses from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bonuses';

import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartBonuses from '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/CartCoins/__pageObject';
import CartBonusItem from '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/CartCoinItem/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import BonusWithTearOff from '@self/root/src/components/BonusWithTearOffControl/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';
import CartGroup from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import BusinessGroupsStrategiesSelector
    from '@self/root/src/widgets/content/cart/CartList/components/BusinessGroupsStrategiesSelector/__pageObject';
import {prepareBonus, prepareItems} from '@self/root/src/spec/hermione/scenarios/loyalty';
import {offerMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {
    offerMock as secondOfferMock,
    skuMock as secondSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/vitaminsLowCost';
import CartAvailableBonusesView from
    '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/View/__pageObject';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

const errorNotSuitable = 'Купон нельзя потратить, у него другие условия';
const errorExpired = 'Ой-ой, у этого купона закончился срок действия';
const errorNotProcessable = 'Что-то пошло не так. Купон не получается потратить на этот заказ';
const errorUnused = 'Оставьте этот купон на будущее – всё равно придётся заплатить по 1 ₽ за товар';

const offer = createOffer(offerMock, offerMock.wareId);
const sku = {
    ...skuMock,
    offers: {
        items: [offerMock],
    },
};
const secondOffer = createOffer(secondOfferMock, secondOfferMock.wareId);
const secondSku = {
    ...secondSkuMock,
    offers: {
        items: [secondOfferMock],
    },
};

const reportState = mergeState([offer]);
const secondReportState = mergeState([offer, secondOffer]);

/**
 * Тесты на ошибки применения купонов в корзине.
 */
export default makeSuite('Ошибки купонов.', {
    feature: 'Ошибки купонов.',
    environment: 'kadavr',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                cartGroup: () => this.createPageObject(CartGroup),
                cartBonuses: () => this.createPageObject(CartBonuses, {root: `${CartBonuses.root}:nth-child(1)`}),
                bonusContainer: () => this.createPageObject(
                    CartBonusItem,
                    {
                        parent: this.cartBonuses,
                        root: `${CartBonusItem.root}:nth-child(1)`,
                    }
                ),
                bonusContainer2: () => this.createPageObject(
                    CartBonusItem,
                    {
                        parent: this.cartBonuses,
                        root: `${CartBonusItem.root}:nth-child(2)`,
                    }
                ),
                bonus: () => this.createPageObject(BonusWithTearOff, {parent: this.bonusContainer}),
                bonus2: () => this.createPageObject(BonusWithTearOff, {parent: this.bonusContainer2}),
                cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                cartItem: () => this.createPageObject(
                    CartItem, {
                        parent: this.cartGroup,
                        root: this.cartItems.firstItem,
                    }
                ),
                cartItem2: () => this.createPageObject(
                    CartItem,
                    {
                        parent: this.cartGroup,
                        root: this.cartItems.secondItem,
                    }
                ),
                removedCartItem: () => this.createPageObject(
                    CartItem,
                    {
                        parent: this.cartGroup,
                        root: `${BusinessGroupsStrategiesSelector.bucket(1, false)} ${CartItem.root}`,
                    }
                ),
                cartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {parent: this.cartItem2}),
                amountSelect: () => this.createPageObject(AmountSelect, {parent: this.cartItem}),
                orderTotal: () => this.createPageObject(OrderTotal),
                toasts: () => this.createPageObject(Notification),
                notification: () => this.createPageObject(Notification),
                bonusParent: () => this.createPageObject(CartAvailableBonusesView),
            });
        },

        'Применение купонов': mergeSuites(
            makeSuite('на фиксированную скидку с ограничением по SKU.', {
                params: {
                    items: 'Офферы корзины',
                    fixedBonus: 'Купон фиксированной скидки с ограничением по SKU [получаем из конфига]',
                },
                defaultParams: {
                    items: [{
                        skuId: secondSkuMock.id,
                        offerId: secondOfferMock.wareId,
                        count: 1,
                    }, {
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 1,
                    }],
                    fixedBonus: bonuses.FIXED_RESTRICTIONS_MSKU,
                    reportSkus: [sku, secondSku],
                },
                story: {
                    async beforeEach() {
                        const carts = [
                            buildCheckouterBucket({
                                items: [{
                                    skuMock: secondSkuMock,
                                    offerMock: secondOfferMock,
                                    count: 1,
                                }, {
                                    skuMock,
                                    offerMock,
                                    count: 1,
                                }],
                            }),
                        ];

                        await this.browser.yaScenario(
                            this,
                            prepareMultiCartState,
                            carts,
                            {existingReportState: secondReportState}
                        );

                        return this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageWithBonus',
                            {
                                items: this.params.items,
                                region: this.params.region,
                                bonuses: {
                                    applicableCoins: [this.params.fixedBonus],
                                },
                                reportSkus: this.params.reportSkus,
                            }
                        );
                    },
                    'При удалении SKU': {
                        'появляется ошибка "Купон нельзя потратить, у него другие условия".': makeCase({
                            id: 'bluemarket-730',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.toasts.isVisible()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Не должно быть сообщений об ошибках бонусов'
                                    );

                                await applyBonus.call(this);

                                await deleteSku.call(this);

                                await this.toasts.isVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должно появится сообщение об ошибках бонусов'
                                    );

                                await this.toasts.getText()
                                    .should.eventually.to.be.contain(
                                        errorNotSuitable,
                                        `Сообщение об ошибке должно содержать текст '${errorNotSuitable}'`
                                    );
                            },
                        }),
                        'купон пропадает.': makeCase({
                            id: 'bluemarket-730',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.isVisible()
                                    .should.eventually.to.be.equal(true, 'Купон должен быть виден');

                                await applyBonus.call(this);

                                await deleteSku.call(this);

                                await this.cartBonuses.isVisible()
                                    .should.eventually.to.be.equal(false, 'Маркет Бонус должен исчезнуть');
                            },
                        }),
                    },
                },
            }),

            makeSuite('на фиксированную скидку с ограничением по сумме заказа.', {
                params: {
                    items: 'Офферы корзины',
                    fixedBonus: 'Купон фиксированной скидки с ограничением по сумме [получаем из конфига]',
                },
                defaultParams: {
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 2,
                        price: 600,
                    }],
                    fixedBonus: bonuses.FIXED_RESTRICTIONS_SUM,
                    reportSkus: [sku],
                },
                story: {
                    async beforeEach() {
                        const carts = [
                            buildCheckouterBucket({
                                items: [{
                                    skuMock,
                                    offerMock,
                                    count: 2,
                                }],
                            }),
                        ];

                        await this.browser.yaScenario(
                            this,
                            prepareMultiCartState,
                            carts,
                            {existingReportState: reportState}
                        );

                        return this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageWithBonus',
                            {
                                items: this.params.items,
                                region: this.params.region,
                                bonuses: {
                                    applicableCoins: [this.params.fixedBonus],
                                },
                                reportSkus: this.params.reportSkus,
                            }
                        );
                    },
                    'При уменьшении суммы заказа': {
                        'появляется ошибка "Купон нельзя потратить, у него другие условия".': makeCase({
                            id: 'bluemarket-731',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.toasts.isVisible()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Не должно быть сообщений об ошибках бонусов'
                                    );

                                await applyBonus.call(this);

                                await reduceAmount.call(this);

                                await this.toasts.isVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должно появится сообщение об ошибках бонусов'
                                    );

                                await this.toasts.getText()
                                    .should.eventually.to.be.equal(
                                        errorNotSuitable,
                                        `Сообщение об ошибке должно содержать текст '${errorNotSuitable}'`
                                    );
                            },
                        }),
                        'купонов пропадает.': makeCase({
                            id: 'bluemarket-731',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.isVisible()
                                    .should.eventually.to.be.equal(true, 'Купон должен быть виден');

                                await applyBonus.call(this);

                                await reduceAmount.call(this);

                                await this.cartBonuses.isVisible()
                                    .should.eventually.to.be.equal(false, 'Купон должен исчезнуть');
                            },
                        }),
                    },
                },
            }),
            makeSuite('на фиксированную скидку с истекшим сроком годности.', {
                params: {
                    items: 'Офферы корзины',
                    fixedBonus: 'Купон фиксированной скидки с истекшим сроком годности [получаем из конфига]',
                },
                defaultParams: {
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 1,
                    }],
                    fixedBonus: bonuses.EXPIRED,
                    reportSkus: [sku],
                },
                story: {
                    async beforeEach() {
                        // в начале бонус не просрочен
                        delete this.params.fixedBonus.endDate;

                        const carts = [
                            buildCheckouterBucket({
                                items: [{
                                    skuMock,
                                    offerMock,
                                    count: 1,
                                }],
                            }),
                        ];

                        await this.browser.yaScenario(
                            this,
                            prepareMultiCartState,
                            carts,
                            {existingReportState: reportState}
                        );

                        return this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageWithBonus',
                            {
                                items: this.params.items,
                                region: this.params.region,
                                bonuses: {
                                    applicableCoins: [this.params.fixedBonus],
                                },
                                reportSkus: this.params.reportSkus,
                            }
                        );
                    },
                    'При применении купона': {
                        'появляется ошибка "Ой-ой, у этого купона закончился срок действия".': makeCase({
                            id: 'bluemarket-2648',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.toasts.isVisible()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Не должно быть сообщений об ошибках бонусов'
                                    );

                                await this.browser.yaScenario(this, prepareBonus, {
                                    bonuses: {
                                        applicableCoins: [bonuses.EXPIRED],
                                    },
                                });

                                await this.bonus.clickOnBonus();

                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.toasts.isVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должно появится сообщение об ошибках бонусов'
                                    );

                                await this.toasts.getText()
                                    .should.eventually.to.be.equal(
                                        errorExpired,
                                        `Сообщение об ошибке должно содержать текст '${errorExpired}'`
                                    );
                            },
                        }),
                        'купон пропадает.': makeCase({
                            id: 'bluemarket-2648',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.isVisible()
                                    .should.eventually.to.be.equal(true, 'Купон должен быть виден');

                                await this.browser.yaScenario(this, prepareBonus, {
                                    bonuses: {
                                        applicableCoins: [bonuses.EXPIRED],
                                    },
                                });

                                await this.bonus.clickOnBonus();

                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.cartBonuses.isVisible()
                                    .should.eventually.to.be.equal(false, 'Купон должен исчезнуть');
                            },
                        }),
                        'в саммари нет скидки по купонам.': makeCase({
                            id: 'bluemarket-2648',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.isVisible()
                                    .should.eventually.to.be.equal(true, 'Купон должен быть виден');
                                await this.browser.yaScenario(this, prepareBonus, {
                                    bonuses: {
                                        applicableCoins: [bonuses.EXPIRED],
                                    },
                                });
                                await this.bonus.clickOnBonus();

                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);
                                await this.orderTotal.isCoinVisible()
                                    .should.eventually.to.be.equal(false, 'Нет скидки по купонам в саммари');
                            },
                        }),
                    },
                },
            }),

            makeSuite('на фиксированную скидку в условиях технической ошибки.', {
                params: {
                    items: 'Офферы корзины',
                    fixedBonus: 'Купон фиксированной скидки который невалиден [получаем из конфига]',
                },
                defaultParams: {
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 1,
                    }],
                    fixedBonus: bonuses.NOT_PROCESSABLE,
                    reportSkus: [sku],
                },
                story: {
                    async beforeEach() {
                        const carts = [
                            buildCheckouterBucket({
                                items: [{
                                    skuMock,
                                    offerMock,
                                    count: 1,
                                }],
                            }),
                        ];

                        await this.browser.yaScenario(
                            this,
                            prepareMultiCartState,
                            carts,
                            {existingReportState: reportState}
                        );

                        return this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageWithBonus',
                            {
                                items: this.params.items,
                                region: this.params.region,
                                bonuses: {
                                    applicableCoins: [this.params.fixedBonus],
                                },
                                reportSkus: this.params.reportSkus,
                            }
                        );
                    },
                    'При применении купона': {
                        'появляется ошибка "Что-то пошло не так".': makeCase({
                            id: 'bluemarket-2649',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.clickOnBonus();

                                await this.notification.waitForNotificationVisible();

                                await this.notification.getText()
                                    .should.eventually.to.be.equal(
                                        errorNotProcessable,
                                        `Сообщение должно содержать текст '${errorNotProcessable}'`
                                    );
                            },
                        }),
                        'купон отщелкивается.': makeCase({
                            id: 'bluemarket-2649',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.clickOnBonus();

                                await this.notification.waitForNotificationVisible();

                                await this.bonus.isChecked()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Купон должен отщелкнуться: чекбокс купона не должен быть чекнут'
                                    );
                            },
                        }),
                    },
                },
            }),

            makeSuite('на сумму больше чем стоимость заказа.', {
                params: {
                    items: 'Офферы корзины',
                    fixedBonuses: 'Купоны фиксированной скидки [получаем из конфига]',
                },
                defaultParams: {
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 1,
                        price: 600,
                    }],
                    fixedBonuses: [
                        bonuses.FIXED,
                        bonuses.FIXED_1000,
                    ],
                    reportSkus: [sku],
                },
                story: {
                    async beforeEach() {
                        const carts = [
                            buildCheckouterBucket({
                                items: [{
                                    skuMock,
                                    offerMock,
                                    count: 1,
                                }],
                            }),
                        ];

                        await this.browser.yaScenario(
                            this,
                            prepareMultiCartState,
                            carts,
                            {existingReportState: reportState}
                        );

                        return this.browser.yaScenario(
                            this,
                            'cart.prepareCartPageWithBonus',
                            {
                                items: this.params.items,
                                region: this.params.region,
                                bonuses: {
                                    applicableCoins: this.params.fixedBonuses,
                                },
                                reportSkus: this.params.reportSkus,
                            }
                        );
                    },
                    'При применении лишнего купона': {
                        'появляется ошибка "Оставьте этот купон на будущее".': makeCase({
                            id: 'bluemarket-733',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);
                                await this.bonus2.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.notification.isVisible()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Сообщение с ошибкой применения купонов не должно отображаться'
                                    );
                                await applyBonus.call(this);

                                await this.bonus2.clickOnBonus();

                                await this.notification.waitForNotificationVisible();

                                await this.notification.getText()
                                    .should.eventually.to.be.equal(
                                        errorUnused,
                                        `Сообщение должно содержать текст '${errorUnused}'`
                                    );
                            },
                        }),
                        'купон отщелкивается.': makeCase({
                            id: 'bluemarket-733',
                            async test() {
                                /**
                                 * Грязный хак
                                 * Временно пока нет тесткейсов для автоприменения, отщелкиваем бонус руками
                                 * Выпил будет тут https://st.yandex-team.ru/MARKETFRONT-18092
                                 */
                                await this.bonus.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);
                                await this.bonus2.clickOnBonus();
                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await applyBonus.call(this);

                                await this.bonus2.clickOnBonus();

                                await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

                                await this.bonus.isChecked()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Купон должен отщелкнуться: чекбокс купона не должен быть чекнут'
                                    );
                            },
                        }),
                    },
                },
            })
        ),
    },
});

async function applyBonus() {
    await this.bonus.clickOnBonus();

    await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);

    await this.bonus.isChecked()
        .should.eventually.to.be.equal(
            true,
            'Купон должен был примениться: чекбокс купона должен быть чекнут'
        );
}

async function deleteSku(index = 0) {
    // Необходимо заранее обновить state лоялти на кадавре для того, чтобы он
    // смог правильно определить доступные бонусы
    this.params.items.splice(index, 1);

    const carts = this.params.items.map(item => buildCheckouterBucket({
        items: [{
            skuMock: item.offerId === offerMock.wareId ? skuMock : secondSkuMock,
            offerMock: item.offerId === offerMock.wareId ? offerMock : secondOfferMock,
            count: item.count || 1,
        }],
    }));

    const {carterItems, checkouterState} = await this.browser.yaScenario(
        this,
        prepareMultiCartState,
        carts,
        {setState: false}
    );

    await this.browser.setState('Checkouter.collections', checkouterState);

    await this.browser.yaScenario(this, prepareItems, {
        items: carterItems,
    });

    await this.cartItem2.clickRemoveButton();

    await this.removedCartItem.waitForVisible();

    await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);
}

async function reduceAmount() {
    // Необходимо заранее обновить state лоялти на кадавре для того, чтобы он
    // смог правильно определить доступные бонусы

    const carts = this.params.items.map(item => buildCheckouterBucket({
        items: [{
            skuMock: item.offerId === offerMock.wareId ? skuMock : secondSkuMock,
            offerMock: item.offerId === offerMock.wareId ? offerMock : secondOfferMock,
            count: (item.count - 1) || 1,
        }],
    }));

    const {carterItems, checkouterState} = await this.browser.yaScenario(
        this,
        prepareMultiCartState,
        carts,
        {setState: false}
    );

    await this.browser.setState('Checkouter.collections', checkouterState);

    await this.browser.yaScenario(this, prepareItems, {
        items: carterItems,
    });

    await this.amountSelect.minusFromButton();

    await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3500), true);
}
