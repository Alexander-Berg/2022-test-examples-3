import {
    makeCase,
    makeSuite,
} from 'ginny';


// import DeliveryInfo from '@self/root/src/components/Orders/DeliveryInfo/__pageObject';
import {OrderHeader} from '@self/root/src/components/OrderHeader/__pageObject';
import {OrderStatus as MyOrderOrderStatus} from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import ReorderButton from '@self/root/src/components/Orders/OrderReorderButton/__pageObject';
import {CancellationButton as CancelButton} from '@self/root/src/components/OrderActions/Actions/CancellationButton/__pageObject';
import {ActionLink} from '@self/root/src/components/OrderActions/Actions/ActionLink/__pageObject';
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
                orderHeader: () => this.createPageObject(OrderHeader, {parent: this.myOrder}),
                myOrderOrderStatus: () => this.createPageObject(MyOrderOrderStatus, {parent: this.myOrder}),
                reorderButton: () => this.createPageObject(ReorderButton, {parent: this.myOrder}),
                cancelButton: () => this.createPageObject(CancelButton, {parent: this.myOrder}),
                orderDocumentsLink: () => this.createPageObject(Link, {
                    parent: this.myOrder,
                    root: ActionLink.documentsLink,
                }),
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

                await this.reorderButton
                    .isVisible()
                    .should.eventually.to.be.equal(
                        Boolean(reorderBtn),
                        `Кнопка пересоздать заказ ${reorderBtn ? '' : 'не '}должна присутствовать на странице`
                    );

                await this.myOrderOrderStatus
                    .getText()
                    .should.eventually.to.have.string(
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

                if (cancelOrder === true || cancelOrder === false) {
                    await this.cancelButton.isExisting()
                        .then(isExist => Boolean(isExist))
                        .catch(() => false)
                        .should.eventually.to.be.equal(cancelOrder,
                            `Кнопка отмены доставки ${cancelOrder ? '' : 'не '}должна быть видна`
                        );
                }
            },
        }),
    },
});
