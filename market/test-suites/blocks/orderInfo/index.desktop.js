import {
    makeCase,
    makeSuite,
} from 'ginny';


import DeliveryInfo from '@self/root/src/components/Orders/DeliveryInfo/__pageObject';
import OrderHeader from '@self/root/src/components/OrderHeader/__pageObject';
import MyOrderOrderStatus from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import OrderDetailsPrice from '@self/root/src/components/Orders/OrderDetailsPrice/__pageObject';
import DetailsPriceContent from '@self/root/src/components/Orders/OrderDetailsPrice/DetailsPriceContent/__pageObject';
import ReorderButton from '@self/root/src/components/Orders/OrderReorderButton/__pageObject';
import CancelButton from '@self/root/src/components/Orders/OrderCancelButton/__pageObject';
import OrderDocumentsLink from '@self/root/src/components/Orders/OrderDocumentsLink/__pageObject';
import OrderSupplierInfoLink from '@self/root/src/components/Orders/OrderSupplierInfoLink/__pageObject';
import OrderCreationDate from '@self/root/src/components/Orders/OrderCreationDate/__pageObject';
import OrderDeliveryService from '@self/root/src/components/OrderDeliveryService/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

/**
 * Для работы сьюита надо чтобы был задан PageObject this.myOrder
 */
module.exports = makeSuite('Информация о заказе', {
    feature: 'Информация о заказе',
    environment: 'kadavr',
    params: {
        trackingId: 'Номер заказа',
        deliveryAddress: 'Адрес доставки',
        recipient: 'Получатель',
        receiptDate: 'Дата получения',
        changeDate: 'Можно ли сменить дату полчения',
        deliveryText: 'Стоимость доставки',
        paymentMethod: 'Способ оплаты',
        price: 'Цена',
        registrationDate: 'Дата оформления',
        reorderBtn: 'Должна ли быть кнопка перезаказать',
        status: 'Статус заказа',
        subStatus: 'Текст под статусом заказа',
        trackCode: 'Трек номер',
        deliveryServiceName: 'Название службы доставки',
        deliveryServicePhone: 'Номер телефона службы доставки',
        deliveryServiceWebsite: 'Сайт службы доставки',
        cancelOrder: 'Можно отменить',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {parent: this.myOrder.orderDetails}),
                orderHeader: () => this.createPageObject(OrderHeader, {parent: this.myOrder}),
                myOrderOrderStatus: () => this.createPageObject(MyOrderOrderStatus, {parent: this.myOrder}),
                myOrderDetailsTitle: () => this.createPageObject(OrderDetailsPrice, {parent: this.myOrder}),
                detailsPriceContent: () => this.createPageObject(DetailsPriceContent, {parent: this.myOrder}),
                reorderButton: () => this.createPageObject(ReorderButton, {parent: this.myOrder}),
                cancelButton: () => this.createPageObject(CancelButton, {parent: this.myOrder}),
                orderDocumentsLink: () => this.createPageObject(OrderDocumentsLink, {parent: this.myOrder}),
                orderSupplierInfoLink: () => this.createPageObject(OrderSupplierInfoLink, {parent: this.myOrder}),
                orderCreationDate: () => this.createPageObject(OrderCreationDate, {parent: this.myOrder}),
                orderDeliveryService: () => this.createPageObject(OrderDeliveryService, {parent: this.myOrder}),
                deliveryServiceWebsite: () => this.createPageObject(Link, {
                    parent: this.orderDeliveryService,
                    root: `${Link.root}${OrderDeliveryService.websiteLink}`,
                }),
                deliveryServicePhone: () => this.createPageObject(Link, {
                    parent: this.orderDeliveryService,
                    root: `${Link.root}${OrderDeliveryService.phoneLink(0)}`,
                }),
            });
        },
        'показываем с ожидаемыми данными': makeCase({
            async test() {
                const {
                    trackingId,
                    deliveryAddress,
                    recipient,
                    receiptDate,
                    changeDate,
                    deliveryText,
                    paymentMethod,
                    price,
                    registrationDate,
                    reorderBtn,
                    status,
                    subStatus,
                    trackCode,
                    deliveryServiceName,
                    deliveryServicePhone,
                    deliveryServiceWebsite,
                    cancelOrder,
                } = this.params;

                await this.orderHeader
                    .getOrderTrackingIdText()
                    .should.eventually.to.have.string(
                        trackingId,
                        `Номер заказа должен быть "${trackingId}"`
                    );

                await this.deliveryInfo.getDeliveryText()
                    .should.eventually.to.have.string(deliveryAddress,
                        'Информация о доставке должна содержать адрес'
                    );

                await this.deliveryInfo.getDeliveryText()
                    .should.eventually.to.have.string(recipient,
                        'Информация о доставке должна содержать получателя'
                    );

                await this.deliveryInfo.getDeliveryText()
                    .should.eventually.to.have.string(receiptDate,
                        'Информация о доставке должна содержать дату получения'
                    );

                if (changeDate === true || changeDate === false) {
                    await this.deliveryInfo.isDeliveryDateTimeDateLinkExist()
                        .should.eventually.to.be.equal(changeDate,
                            `Кнопка об изменении даты доставки ${changeDate ? '' : 'не '}должна быть видна`
                        );
                }

                if (cancelOrder === true || cancelOrder === false) {
                    await this.cancelButton.isExisting()
                        .then(isExist => Boolean(isExist))
                        .catch(() => false)
                        .should.eventually.to.be.equal(cancelOrder,
                            `Кнопка отмены доставки ${cancelOrder ? '' : 'не '}должна быть видна`
                        );
                }

                await this.deliveryInfo.getDeliveryText()
                    .should.eventually.to.have.string(deliveryText,
                        'Информация о доставке должна содержать стоимость доставки'
                    );

                await this.myOrderDetailsTitle.getPaymentMethod()
                    .should.eventually.to.be.equal(
                        paymentMethod,
                        `Способ оплаты должен быть "${paymentMethod}"`
                    );

                await this.detailsPriceContent.getText()
                    .should.eventually.to.be.equal(
                        price,
                        `Блок с ценой должен быть равен "${price}"`
                    );

                await this.reorderButton
                    .isVisible()
                    .should.eventually.to.be.equal(
                        Boolean(reorderBtn),
                        `Кнопка пересоздать заказ ${reorderBtn ? '' : 'не '}должна присутствовать на странице`
                    );

                await this.myOrderOrderStatus
                    .getStatusText()
                    .should.eventually.be.equal(
                        status,
                        `Статус заказа должен быть "${status}"`
                    );

                if (subStatus) {
                    await this.myOrderOrderStatus
                        .getDeliveryInfoText()
                        .should.eventually.be.equal(
                            subStatus,
                            `Текст под статусом заказа должен быть "${subStatus}"`
                        );
                }

                await this.orderDocumentsLink
                    .isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Ссылка "Документы по заказу" должна отображаться'
                    );

                await this.orderSupplierInfoLink
                    .isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Ссылка "Информация о товаре и продавце" должна отображаться'
                    );

                await this.orderCreationDate
                    .getDateText()
                    .should.eventually.to.be.equal(
                        registrationDate,
                        `Дата оформления заказа должна быть: "${registrationDate}"`
                    );

                if (trackCode || deliveryServiceName || deliveryServicePhone || deliveryServiceWebsite) {
                    await this.orderHeader.clickOrderTrackingAction();

                    await this.orderDeliveryService.waitForExist();
                }

                if (trackCode) {
                    const trackText = `код вашей посылки — ${trackCode}`;
                    await this.orderDeliveryService.getText()
                        .should.eventually.to.have.string(trackText,
                            'В информации о том кто доставляет должен быть trackCode'
                        );
                }

                if (deliveryServiceName) {
                    await this.orderDeliveryService.getText()
                        .should.eventually.to.have.string(deliveryServiceName,
                            'В информации о том кто доставляет должно быть имя службы'
                        );
                }

                if (deliveryServicePhone) {
                    await this.deliveryServicePhone.getText()
                        .should.eventually.to.be.equal(
                            deliveryServicePhone,
                            'В информации о том кто доставляет должен быть телефон службы'
                        );
                }

                if (deliveryServiceWebsite) {
                    await this.deliveryServiceWebsite.getHref()
                        .should.eventually.to.be.equal(
                            deliveryServiceWebsite,
                            'В информации о том кто доставляет должен быть сайт службы'
                        );
                }
            },
        }),
    },
});
