import {
    makeCase,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {checkpoints} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/checkpoints';

import OrderHeader from '@self/root/src/components/OrderHeader/__pageObject';
import OrderCheckpoint from '@self/root/src/components/OrderHeader/OrderCheckpoint/__pageObject';

const orderCreationTime = {
    rawValue: '20-04-2020 11:45:10',
    timestamp: 1587372310000,
    displayedValue: '20 апреля 2020, 11:45',
};

module.exports = makeSuite('Трекинг.', {
    feature: 'Трекинг',
    environment: 'kadavr',
    id: 'bluemarket-3256',
    issue: 'BLUEMARKET-9971',
    story: {
        beforeEach() {
            this.setPageObjects({
                orderHeader: () => this.createPageObject(OrderHeader, {
                    parent: this.myOrder,
                }),
                firstCheckpoint: () => this.createPageObject(OrderCheckpoint, {
                    parent: this.orderHeader,
                    root: `${OrderCheckpoint.root}:nth-child(1)`,
                }),
                secondCheckpoint: () => this.createPageObject(OrderCheckpoint, {
                    parent: this.orderHeader,
                    root: `${OrderCheckpoint.root}:nth-child(2)`,
                }),
            });
        },

        'Кнопка "Отследить".': {
            ['Для заказа с чекпоинтами отображается, по клику разворачивается список чекпоинтов, ' +
                'на кнопке меняется текст на "Свернуть", по клику список сворачивается.']: makeCase({
                async test() {
                    await createOrder.call(this, {status: 'DELIVERY'});

                    await this.expect(await this.orderHeader.getOrderTrackingActionVisibility())
                        .to.be.equal(true, 'Кнопка трекинга должна быть видна');

                    await this.expect(await this.firstCheckpoint.getVisibility())
                        .to.be.equal(false, 'Чекпоинты не должны быть видны');

                    const expectedText1 = 'Отследить';
                    await this.orderHeader.getOrderTrackingActionText()
                        .should.eventually.to.be.equal(expectedText1, `Текст на кнопке должен быть ${expectedText1}`);

                    await this.orderHeader.clickOrderTrackingAction();

                    await this.firstCheckpoint.waitForVisible();

                    const expectedText2 = 'Свернуть';
                    await this.orderHeader.getOrderTrackingActionText()
                        .should.eventually.to.be.equal(expectedText2, `Текст на кнопке должен быть ${expectedText2}`);

                    await this.orderHeader.clickOrderTrackingAction();

                    await this.firstCheckpoint.waitForHidden();
                },
            }),

            'Для заказа в статусе UNPAID не отображается': makeCase({
                async test() {
                    await createOrder.call(this, {status: 'UNPAID'});

                    await this.expect(await this.orderHeader.getOrderTrackingActionVisibility())
                        .to.be.equal(false, 'Кнопка трекинга не должна быть видна');
                },
            }),

            'Для заказа в статусе CANCELLED не отображается': makeCase({
                async test() {
                    await createOrder.call(this, {status: 'CANCELLED'});

                    await this.expect(await this.orderHeader.getOrderTrackingActionVisibility())
                        .to.be.equal(false, 'Кнопка трекинга не должна быть видна');
                },
            }),

            'Для заказа в статусе PENDING не отображается': makeCase({
                async test() {
                    await createOrder.call(this, {status: 'PENDING'});

                    await this.expect(await this.orderHeader.getOrderTrackingActionVisibility())
                        .to.be.equal(false, 'Кнопка трекинга не должна быть видна');
                },
            }),
        },

        'Список чекпоинтов.': {
            async beforeEach() {
                await createOrder.call(this, {status: 'DELIVERY'});
                await this.orderHeader.clickOrderTrackingAction();
                await this.firstCheckpoint.waitForVisible();
            },

            'Отображается правильное количество чекпоинтов + дата создания заказа': makeCase({
                async test() {
                    const count = checkpoints.length + 1;
                    await this.orderHeader.getCheckpointsCount()
                        .should.eventually.to.be.equal(count, `Чекпоинтов должно быть ${count}`);
                },
            }),

            'Чекпоинт содержит статус': makeCase({
                async test() {
                    await this.firstCheckpoint.status.isVisible()
                        .should.eventually.to.be.equal(true, 'Статус должен быть виден');

                    const status = 'Заказ создан';
                    await this.firstCheckpoint.getStatusText()
                        .should.eventually.to.be.equal(status, `Статус должен быть "${status}"`);
                },
            }),

            'Чекпоинт содержит дату': makeCase({
                async test() {
                    await this.firstCheckpoint.time.isVisible()
                        .should.eventually.to.be.equal(true, 'Дата должна быть видна');

                    const time = orderCreationTime.displayedValue;
                    await this.firstCheckpoint.getTimeText()
                        .should.eventually.to.be.equal(time, `Дата должна быть "${time}"`);
                },
            }),
        },
    },
});

async function createOrder({status}) {
    const {browser} = this;
    const orderId = 123456;

    await browser.yaScenario(
        this,
        prepareOrder,
        {
            orders: [{
                orderId,
                deliveryType: 'DELIVERY',
                delivery: {
                    shipments: [{
                        tracks: [
                            {
                                checkpoints,
                            },
                        ],
                    }],
                    tracks: [
                        {
                            checkpoints,
                        },
                    ],
                },
            }],
            status,
            creationDate: orderCreationTime.rawValue,
            creationDateTimestamp: orderCreationTime.timestamp,
        }
    );

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        return this.browser.yaOpenPage(this.params.pageId, {orderId});
    }

    return this.browser.yaOpenPage(this.params.pageId);
}
