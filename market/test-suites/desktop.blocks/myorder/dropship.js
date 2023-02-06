import {
    makeCase,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

import CancelSubstatusRadioControl
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancelSubstatusRadioControl/__pageObject';
import MyOrderCancelButton from '@self/root/src/components/Orders/OrderCancelButton/__pageObject';
import MyOrderOrderStatus from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import CancelPopupBody from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancelPopupBody/__pageObject';
import CancellationRequestForm
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancellationRequestForm/__pageObject';
import CancellationRequestSuccess
    from '@self/root/src/widgets/content/orders/MyOrderCancel/components/CancellationRequestSuccess/__pageObject';
import DeliveryInfo from '@self/root/src/components/Orders/DeliveryInfo/__pageObject';
import CreationDate from '@self/root/src/components/Orders/OrderCreationDate/__pageObject';

module.exports = makeSuite('Дропшип.', {
    feature: 'Дропшип',
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                myOrderCancelButton: () => this.createPageObject(MyOrderCancelButton, {parent: this.myOrder}),
                myOrderOrderStatus: () => this.createPageObject(MyOrderOrderStatus, {parent: this.myOrder}),
                cancelPopupBody: () => this.createPageObject(CancelPopupBody),
                cancellationRequestForm: () => this.createPageObject(
                    CancellationRequestForm,
                    {parent: this.cancelPopupBody}
                ),
                cancelSubstatusRadioControl: () => this.createPageObject(
                    CancelSubstatusRadioControl,
                    {
                        parent: this.cancellationRequestForm,
                        root: `${CancelSubstatusRadioControl.root}:nth-child(1)`,
                    }
                ),
                cancellationRequestSuccess: () => this.createPageObject(
                    CancellationRequestSuccess,
                    {parent: this.cancelPopupBody}
                ),
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {parent: this.myOrder.orderDetails}),
                creationDate: () => this.createPageObject(CreationDate, {parent: this.myOrder.orderDetails}),
            });
        },

        'По умолчанию': {
            beforeEach() {
                return createOrder.call(this, {status: 'PROCESSING'});
            },

            'отмена должна пройти мгновенно': makeCase({
                id: 'bluemarket-2478',
                issue: 'BLUEMARKET-3637',
                async test() {
                    await this.myOrderCancelButton.click();
                    await this.cancelPopupBody
                        .waitForPopupIsVisible()
                        .should.eventually.to.be.equal(true, 'Попап отмены должен быть виден');

                    await this.cancelSubstatusRadioControl.clickLabel();
                    await this.cancellationRequestForm.clickCancelButton();

                    await this.cancellationRequestSuccess
                        .waitForSuccessIsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Сообщение об успешном создании заявки на отмену должно быть видимым'
                        );

                    await this.cancellationRequestSuccess
                        .getTitleText()
                        .should.eventually.to.have.string(
                            'отменён',
                            'В заголовке должно быть указано, что заказ отменён'
                        );

                    await this.cancellationRequestSuccess.clickContinueButton();

                    await this.cancelPopupBody
                        .waitForPopupIsHidden()
                        .should.eventually.to.be.equal(true, 'Попап отмены должен быть скрыт');

                    await this.myOrderOrderStatus
                        .getStatusText()
                        .should.eventually.to.be.equal(
                            'Отменён',
                            'Текст статуса должен замениться на "Отменён"'
                        );
                },
            }),

            'в информации о доставке есть пункт "Адрес"': makeCase({
                id: 'bluemarket-2478',
                issue: 'BLUEMARKET-3637',
                async test() {
                    await this.deliveryInfo.isVisible()
                        .should.eventually.to.be.equal(true, 'Информация о доставке должна быть видна');

                    const text = 'Адрес';
                    await this.deliveryInfo.getDeliveryText()
                        .should.eventually.to.have.string(text, `Информация о доставке должна содержать "${text}"`);
                },
            }),

            'нет надписи "При получении может потребоваться паспорт"': makeCase({
                id: 'bluemarket-2718',
                issue: 'BLUEMARKET-5989',
                async test() {
                    await this.creationDate.isExisting(this.creationDate.receiptNotes)
                        .should.eventually.be.equal(false, 'Надписи не должно быть');
                },
            }),

            'дата получения': makeCase({
                id: 'bluemarket-2718',
                issue: 'BLUEMARKET-5989',
                async test() {
                    await this.deliveryInfo.getOrderDeliveryDateTimeContentText()
                        .should.eventually.to.be.include('Самовывоз',
                            'Надпись должна содержать "Самовывоз"');
                },
            }),
        },

        'В статусе PICKUP': {
            beforeEach() {
                return createOrder.call(this, {status: 'PICKUP', substatus: 'PICKUP_SERVICE_RECEIVED', rgb: 'BLUE'});
            },

            'статус заказа "Можно получить"': makeCase({
                id: 'bluemarket-2718',
                issue: 'BLUEMARKET-5989',
                async test() {
                    return this.myOrderOrderStatus
                        .getStatusText()
                        .should.eventually.to.be.equal(
                            'Можно получить',
                            'Текст статуса должен быть "Можно получить"'
                        );
                },
            }),
        },
    },
});

async function createOrder({status, substatus, rgb} = {}) {
    const {browser} = this;

    const order = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status,
            substatus,
            region: this.params.region,
            rgb,
            orders: [{
                items: [{skuId: checkoutItemIds.dropship.skuId}],
                deliveryType: 'PICKUP',
                // Признак фармы start
                delivery: {
                    deliveryPartnerType: 'SHOP',
                },
                // Признак фармы end
            }],
            paymentType: 'POSTPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
            // Признак фармы start
            fulfilment: false,
            // Признак фармы end
        }
    );

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        const orderId = _.get(order, ['orders', 0, 'id']);

        return this.browser.yaOpenPage(this.params.pageId, {orderId});
    }

    return this.browser.yaOpenPage(this.params.pageId);
}
