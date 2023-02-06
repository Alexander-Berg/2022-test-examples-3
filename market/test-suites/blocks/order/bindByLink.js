import {
    makeSuite,
    makeCase,
    PageObject,
} from 'ginny';

import OrderBinderNotification
    from '@self/root/src/widgets/content/orders/OrderBinder/components/OrderBinderNotification/__pageObject';
import InformationPanel from '@self/root/src/components/InformationPanel/__pageObject';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

module.exports = makeSuite('Привязка заказа по ссылке.', {
    feature: 'Привязка заказа',
    id: 'bluemarket-3337',
    issue: 'BLUEMARKET-10198',
    environment: 'kadavr',
    params: {
        orderId: 'ID заказа',
        bindKey: 'Ключ привязки заказа',
    },
    defaultParams: {
        orderId: generateRandomId(),
        bindKey: `${generateRandomId()}`,
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderBinderNotification: () => this.createPageObject(OrderBinderNotification),
                informationPanel: () => this.createPageObject(InformationPanel, {
                    parent: this.orderBinderNotification,
                }),
                // отдельный pageObject для проверки на !isExisting
                informationPanelContent: () => this.createPageObject(PageObject, {
                    root: InformationPanel.content,
                    parent: this.informationPanel,
                }),
            });
        },
        'Успешная привязка.': {
            'За заказ выданы купоны.': {
                'Все купоны успешно привязались.': {
                    async beforeEach() {
                        await prepareOrderForBinding.call(this, {totalBonusCount: 2, successfulBonusBindCount: 2});
                    },

                    'Заголовок оповещения': {
                        'содержит текст об успешно привязанном заказе': makeCase({
                            async test() {
                                await this.orderBinderNotification.isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Оповещение о привязке заказа должно отображаться'
                                    );

                                return this.informationPanel.getTitleText()
                                    .should.eventually.be.equal(
                                        `Заказ №${this.params.orderId} привязан к вашему профилю`,
                                        'Заголовок оповещения должен содержать корректный текст'
                                    );
                            },
                        }),
                    },

                    'Контент оповещения': {
                        'содержит правильный текст': makeCase({
                            test() {
                                return this.informationPanel.getContentText()
                                    .should.eventually.be.equal(
                                        'Купоны добавлены в коллекцию.',
                                        'Контент оповещения должен содержать текст об успешно привязанных купонах'
                                    );
                            },
                        }),
                    },
                },
                'Некоторые купоны не привязались.': {
                    async beforeEach() {
                        await prepareOrderForBinding.call(this, {totalBonusCount: 2, successfulBonusBindCount: 1});
                    },

                    'Контент оповещения': {
                        'содержит правильный текст': makeCase({
                            test() {
                                return this.informationPanel.getContentText()
                                    .should.eventually.be.equal(
                                        'Купоны скоро появятся в коллекции.',
                                        'Контент оповещения должен содержать текст о том, что купоны привяжутся позже'
                                    );
                            },
                        }),
                    },
                },
            },
            'За заказ не выданы купоны.': {
                async beforeEach() {
                    await prepareOrderForBinding.call(this, {totalBonusCount: 0});
                },

                'Контент оповещения': {
                    'не отображается': makeCase({
                        test() {
                            return this.informationPanelContent.isExisting()
                                .should.eventually.be.equal(
                                    false,
                                    'Контент оповещения не должен отображаться'
                                );
                        },
                    }),
                },
            },
        },
    },
});

async function prepareOrderForBinding({totalBonusCount, successfulBonusBindCount}) {
    const {orderId, bindKey} = this.params;

    const result = await this.browser.yaScenario(
        this,
        prepareOrder,
        {
            status: 'DELIVERED',
            region: this.params.region,
            orders: [{
                orderId,
                items: [{skuId: checkoutItemIds.asus.skuId}],
                deliveryType: 'DELIVERY',
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
            additionalKadavrCollections: {},
        }
    );

    await this.browser.setState('Checkouter.collections.orderBind', [
        {
            ...result.orders[0],
            bindKey,
            bindInfo: {
                status: 'SUCCESS',
                bonuses: totalBonusCount > 0
                    ? {
                        totalCount: totalBonusCount,
                        successfulBindCount: successfulBonusBindCount,
                    }
                    : undefined,
            },
        },
    ]);

    return this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS, {orderId, bindKey});
}
