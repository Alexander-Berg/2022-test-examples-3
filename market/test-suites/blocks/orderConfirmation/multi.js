import {makeCase, makeSuite} from 'ginny';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import formDataPost from '@self/root/src/spec/hermione/configs/checkout/formData/user-post-postpaid-rostov';
import formDataDelivery from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid-rostov';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {skuMock as kettleSkuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {skuMock as televizorSkuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {rostovOutlet} from '@self/root/src/spec/hermione/kadavr-mock/report/multicheckout';
import OrderDetailsTitle from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderTitle/__pageObject';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {DELIVERY_PARTNER_TYPE} from '@self/root/src/entities/delivery';

const FIRST_ORDER_ID = 11111;
const SECOND_ORDER_ID = 22222;

const firstOrder = {
    orderId: FIRST_ORDER_ID,
    items: [{
        skuId: kettleSkuMock.id,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: formDataPost.recipient,
    deliveryType: 'POST',
    outletId: rostovOutlet.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 100,
        dates: {
            fromDate: '10-10-2000',
            toDate: '15-10-2000',
            fromTime: '13:00',
            toTime: '19:00',
        },
        deliveryPartnerType: DELIVERY_PARTNER_TYPE.SHOP,
    },
};

const secondOrder = {
    orderId: SECOND_ORDER_ID,
    items: [{
        skuId: televizorSkuMock.id,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: formDataDelivery.recipient,
    deliveryType: 'DELIVERY',
    address: formDataDelivery.address,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 200,
        dates: {
            fromDate: '11-11-2000',
            toDate: '11-11-2000',
            fromTime: '12:00',
            toTime: '22:00',
        },
        deliveryPartnerType: DELIVERY_PARTNER_TYPE.SHOP,
    },
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Спасибо за мультизаказ.', {
    id: 'bluemarket-2586',
    issue: 'BLUEMARKET-5434',
    environment: 'kadavr',
    feature: 'Мультизаказ',
    params: {
        region: 'Регион',
        regionName: 'Название региона',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderConfirmation: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                }),
                firstOrderDetails: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.firstDelivery}`,
                }),
                firstOrderDetailsTitle: () => this.createPageObject(OrderDetailsTitle, {
                    parent: this.confirmationPage,
                    root: OrderConfirmation.firstDelivery,
                }),
                firstOrderConfirmationLink: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.firstDelivery} ${OrderConfirmation.detailsLink}`,
                }),

                secondOrderDetails: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.secondDelivery}`,
                }),
                secondOrderDetailsTitle: () => this.createPageObject(OrderDetailsTitle, {
                    parent: this.confirmationPage,
                    root: OrderConfirmation.secondDelivery,
                }),
                secondOrderConfirmationLink: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.secondDelivery} ${OrderConfirmation.detailsLink}`,
                }),
            });

            const defaultState = mergeState([
                {
                    data: {
                        results: [
                            rostovOutlet,
                        ],
                        search: {results: []},
                    },
                },
            ]);

            await this.browser.yaScenario(this, setReportState, {state: defaultState});

            await this.browser.yaPageReloadExtended();
        },

        'Предоплатный заказ.': {
            async beforeEach() {
                await this.browser.yaScenario(this, 'thank.prepareThankPage', {
                    orders: [firstOrder, secondOrder],
                    region: region[this.params.regionName],
                    paymentOptions: {
                        paymentType: 'PREPAID',
                        paymentMethod: 'YANDEX',
                        paymentStatus: 'HOLD',
                        status: 'PROCESSING',
                    },
                });
            },

            'URL правильный': makeCase({
                async test() {
                    await this.browser.getUrl()
                        .should.eventually.to.be.link({
                            pathname: /^\/my\/orders\/confirmation$/,
                            query: {
                                orderId: [FIRST_ORDER_ID, SECOND_ORDER_ID],
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),

            'Ссылка на чек': makeCase({
                async test() {
                    await this.orderConfirmation.receiptLink.isVisible()
                        .should.eventually.to.be.equal(true, 'Ссылка на чек должна быть видна');

                    await this.orderConfirmation.getReceiptLinkUrl()
                        .should.eventually.to.be.link({
                            pathname: `/my/order/${FIRST_ORDER_ID}/`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),

            'Проверяем заполнение данных': {
                'в заголовках': makeCase({
                    async test() {
                        const header = 'Все посылки оформлены!';
                        await this.orderConfirmation.getOrdersTitle()
                            .should.eventually.to.be.equal(
                                header,
                                `Заголовок страницы должен быть ${header}`
                            );

                        const firstOrderTitle = 'первая посылка';
                        await this.firstOrderDetailsTitle.getTitleText()
                            .should.eventually.to.be.equal(
                                firstOrderTitle,
                                `Заголовок первой посылки должен быть "${firstOrderTitle}"`
                            );

                        const secondOrderTitle = 'вторая посылка';
                        await this.secondOrderDetailsTitle.getTitleText()
                            .should.eventually.to.be.equal(
                                secondOrderTitle,
                                `Заголовок второй посылки должен быть "${secondOrderTitle}"`
                            );
                    },
                }),

                'в первой посылке': makeCase({
                    async test() {
                        await this.firstOrderDetails.isVisible()
                            .should.eventually.be.equal(true, 'Первая посылка должна быть видима');

                        await this.firstOrderConfirmationLink.detailsClick();

                        await this.firstOrderDetails.getOrderNumberText()
                            .should.eventually.be.equal(
                                `${FIRST_ORDER_ID}`,
                                `Номер заказа первой посылки должн быть ${FIRST_ORDER_ID}`
                            );

                        await this.firstOrderDetails.getDeliveryRecipientText()
                            .should.eventually.be.equal(
                                `${formDataPost.recipient.lastName} ${formDataPost.recipient.firstName} ${formDataPost.recipient.middleName}, ` +
                                `${formDataPost.recipient.email}, ${formDataPost.recipient.phone}`,
                                'Данные покупателя первой посылки должны соответствовать введенным'
                            );

                        const paymentMethodText = 'Картой онлайн';
                        await this.firstOrderDetails.getDeliveryPaymentMethodText()
                            .should.eventually.be.equal(
                                paymentMethodText,
                                `Способ оплаты первой посылки должен быть ${paymentMethodText}`
                            );

                        await this.firstOrderDetails.getOutletAddressText()
                            .should.eventually.be.equal(
                                rostovOutlet.address.fullAddress,
                                `Адрес доставки первой посылки должен быть ${rostovOutlet.address.fullAddress}`
                            );

                        const deliveryText = 'Доставка продавца 10–15 октября с 13:00 до 19:00';
                        await this.firstOrderDetails.getOrderDeliveryText()
                            .should.eventually.be.equal(
                                deliveryText,
                                `Дата и время доставки первой посылки должены быть "${deliveryText}"`
                            );
                    },
                }),

                'во второй посылке': makeCase({
                    async test() {
                        await this.secondOrderDetails.isVisible()
                            .should.eventually.be.equal(true, 'Вторая посылка должна быть видима');

                        await this.secondOrderConfirmationLink.detailsClick();

                        await this.secondOrderDetails.getOrderNumberText()
                            .should.eventually.be.equal(
                                `${SECOND_ORDER_ID}`,
                                `Номер заказа второй посылки должн быть ${SECOND_ORDER_ID}`
                            );

                        await this.secondOrderDetails.getDeliveryAddressText()
                            .should.eventually.be.equal(
                                `${this.params.regionName}, ${formDataDelivery.address.street}, д. ${formDataDelivery.address.house}, ${formDataDelivery.address.apartment}`,
                                'Адрес доставки второй посылки должен соответствовать введенному'
                            );

                        await this.secondOrderDetails.getDeliveryRecipientText()
                            .should.eventually.be.equal(
                                `${formDataDelivery.recipient.lastName} ${formDataDelivery.recipient.firstName}, ` +
                                `${formDataDelivery.recipient.email}, ${formDataDelivery.recipient.phone}`,
                                'Данные покупателя второй посылки должны соответствовать введенным'
                            );

                        const paymentMethodText = 'Картой онлайн';
                        await this.secondOrderDetails.getDeliveryPaymentMethodText()
                            .should.eventually.be.equal(
                                paymentMethodText,
                                `Способ оплаты второй посылки должен быть ${paymentMethodText}`
                            );

                        const deliveryText = 'Доставка продавца в субботу, 11 ноября с 12:00 до 22:00';
                        await this.secondOrderDetails.getOrderDeliveryText()
                            .should.eventually.be.equal(
                                deliveryText,
                                `Дата и время доставки второй посылки должены быть "${deliveryText}"`
                            );
                    },
                }),

                'Проверяем отображение статусов оплаты': makeCase({
                    async test() {
                        const firstPaymentStatusText = 'Оплачено';
                        await this.firstOrderDetails.getOrderPaymentStatusText()
                            .should.eventually.be.equal(
                                firstPaymentStatusText,
                                `Статус оплаты первой посылки должен быть "${firstPaymentStatusText}"`
                            );

                        const secondPaymentStatusText = 'Оплачено';
                        await this.secondOrderDetails.getOrderPaymentStatusText()
                            .should.eventually.be.equal(
                                secondPaymentStatusText,
                                `Статус оплаты второй посылки должен быть "${secondPaymentStatusText}"`
                            );
                    },
                }),
            },
        },

        'Постоплатный заказ.': {
            async beforeEach() {
                await this.browser.yaScenario(this, 'thank.prepareThankPage', {
                    orders: [firstOrder, secondOrder],
                    region: region[this.params.regionName],
                    paymentOptions: {
                        paymentType: 'POSTPAID',
                        paymentMethod: 'CASH_ON_DELIVERY',
                        paymentStatus: 'HOLD',
                        status: 'PROCESSING',
                    },
                });
            },

            'Проверяем отображение цены': makeCase({
                async test() {
                    const firstPaymentStatusText = 'Оплата при получении 105 ₽';
                    await this.firstOrderDetails.getOrderPaymentStatusText()
                        .should.eventually.be.equal(
                            firstPaymentStatusText,
                            `Статус оплаты первой посылки должен быть "${firstPaymentStatusText}"`
                        );

                    const secondPaymentStatusText = 'Оплата при получении 205 ₽';
                    await this.secondOrderDetails.getOrderPaymentStatusText()
                        .should.eventually.be.equal(
                            secondPaymentStatusText,
                            `Статус оплаты второй посылки должен быть "${secondPaymentStatusText}"`
                        );
                },
            }),
        },
    },
});
