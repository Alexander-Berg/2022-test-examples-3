import {
    makeCase,
    makeSuite,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {ActionLink} from '@self/root/src/components/OrderActions/Actions/ActionLink/__pageObject';


module.exports = makeSuite('Документы.', {
    feature: 'Документы',
    story: {
        beforeEach() {
            this.setPageObjects({
                returnDocumentsLink: () => this.createPageObject(
                    ActionLink,
                    {
                        parent: this.orderCard,
                        root: `${ActionLink.root}${ActionLink.documentsLink}`,
                    }
                ),
            });
        },
        'Ссылка "Документы по заказу" для постоплатного заказа': {
            'в статусе "Отменён"': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'POSTPAID',
                        status: 'CANCELLED',
                    });
                },

                'не должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2624',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Ссылка "Документы по заказу" ' +
                                'не должна отображаться');
                    },
                }),
            },
            'в статусе "Уже у вас"': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'POSTPAID',
                        status: 'DELIVERED',
                    });
                },

                'должна отобразиться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2623',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Ссылка "Документы по заказу" ' +
                                'должна отобразиться');
                    },
                }),
            },
            'в статусе "Уже в пути"': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'POSTPAID',
                        status: 'DELIVERY',
                    });
                },

                'не должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2622',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Ссылка "Документы по заказу" ' +
                                'не должна отображаться');
                    },
                }),
            },
            'в статусе "Уже обрабатывается"': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'POSTPAID',
                        status: 'PROCESSING',
                    });
                },

                'не должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2621',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Ссылка "Документы по заказу" ' +
                                'не должна отображаться');
                    },
                }),
            },
        },
        'Ссылка "Документы по заказу" для предоплатного заказа': {
            'в статусе "Еще не оплачен"': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'PREPAID',
                        status: 'UNPAID',
                    });
                },

                'не должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2620',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Ссылка "Документы по заказу" ' +
                                'не должна отображаться');
                    },
                }),
            },
            'в статусе "Отменен"': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'PREPAID',
                        status: 'CANCELLED',
                    });
                },

                'не должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2619',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Ссылка "Документы по заказу" ' +
                                'не должна отображаться');
                    },
                }),
            },
            'в статусе "Отменен" и при этом оплачен': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'PREPAID',
                        paymentStatus: 'HOLD',
                        status: 'CANCELLED',
                    });
                },

                'должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2618',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Ссылка "Документы по заказу" ' +
                                'должна отображаться');
                    },
                }),
            },
            'при успешной транкзакции': {
                beforeEach() {
                    return createOrder.call(this, {
                        paymentType: 'PREPAID',
                        paymentStatus: 'HOLD',
                        status: 'PROCESSING',
                    });
                },

                'должна отображаться': makeCase({
                    issue: 'BLUEMARKET-5812',
                    id: 'bluemarket-2615',
                    environment: 'kadavr',
                    async test() {
                        return this.returnDocumentsLink
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Ссылка "Документы по заказу" ' +
                                'должна отображаться');
                    },
                }),
            },
        },
    },
});


async function createOrder({status, paymentType, paymentMethod, paymentStatus}) {
    const {browser} = this;

    const order = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status,
            paymentType,
            paymentMethod,
            paymentStatus,
            region: this.params.region,
            orders: [{
                items: [{skuId: checkoutItemIds.asus.skuId}],
                deliveryType: 'DELIVERY',
            }],
        }
    );

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        const orderId = order.orders[0].id;

        return this.browser.yaOpenPage(this.params.pageId, {orderId});
    }

    return this.browser.yaOpenPage(this.params.pageId);
}
