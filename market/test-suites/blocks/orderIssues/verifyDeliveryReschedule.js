import assert from 'assert';
import {
    makeCase,
    makeSuite,
} from 'ginny';
import {
    mergeState,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

import VerifyDeliveryReschedulePopup
    from '@self/root/src/widgets/content/orderIssues/VerifyDeliveryReschedulePopup/__pageObjects';
import YandexMessenger from '@self/root/src/widgets/core/YandexMessenger/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_AGITATION_TYPE} from '@self/root/src/constants/orderAgitation';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareNotificationAgitation} from '@self/root/src/spec/hermione/scenarios/persAuthorResource';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {mockOrderConsultationsState} from '@self/root/src/spec/hermione/scenarios/orderConsultation';


export default makeSuite('Попап подтверждения переноса доставки пользователем', {
    environment: 'kadavr',
    feature: 'Подтверждение переноса доставки DSBS',
    issue: 'MARKETFRONT-37686',
    defaultParams: {
        isAuthWithPlugin: true,
        isAuth: true,
    },
    story: {
        async beforeEach() {
            assert(
                this.params.pageId,
                'Param pageId must be defined in order to run this suite'
            );

            this.setPageObjects({
                verifyDeliveryReschedulePopup: () => this.createPageObject(VerifyDeliveryReschedulePopup),
                yandexMessenger: () => this.createPageObject(YandexMessenger),
            });
        },
        'Видимость элементов попапа.': {
            async beforeEach() {
                await prepareStateAndNavigate.call(this);

                return this.verifyDeliveryReschedulePopup.waitForVisibleRoot();
            },
            'В попапе подтверждения отмены должны отображаться товары из заказа': makeCase({
                async test() {
                    await this.verifyDeliveryReschedulePopup.isPopupVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап подтверждения отмены отображается'
                        );

                    await this.verifyDeliveryReschedulePopup.areOrderItemsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Товары из заказа отображаются'
                        );

                    await this.verifyDeliveryReschedulePopup.isHeaderVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Заголовок отображается'
                        );

                    await this.verifyDeliveryReschedulePopup.isDescriptionVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Описание отображается'
                        );

                    await this.verifyDeliveryReschedulePopup.getOrderItemsCount()
                        .should.eventually.to.be.equal(
                            2,
                            'Товаров из заказа два'
                        );

                    await this.verifyDeliveryReschedulePopup.areButtonsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кнопки подтверждения и неподтверждения отображаются'
                        );

                    await this.verifyDeliveryReschedulePopup.getNthOrderItemCount(1)
                        .should.eventually.to.be.equal(
                            8,
                            'У второго товара отображается бейдж с количеством 8'
                        );
                },
            }),
        },
        'Функциональность попапа.': {
            async beforeEach() {
                this.yaTestData = this.yaTestData || {};
                this.yaTestData.currentOrderId = await prepareStateAndNavigate.call(this);

                return this.verifyDeliveryReschedulePopup.waitForVisibleRoot();
            },
            'В попапе подтверждения отмены': {
                'по клику на кнопку подтверждения': {
                    'появляется окно с информацией о результате, отправляется запрос на завершение агитации': makeCase({
                        async test() {
                            await checkAgitationCompleted.call(this, () =>
                                this.verifyDeliveryReschedulePopup.clickAcceptButton()
                            );

                            await this.verifyDeliveryReschedulePopup.areResultsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран результатов отображается'
                                );

                            await this.verifyDeliveryReschedulePopup.areOrderItemsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Товары из заказа отображаются'
                                );

                            await this.verifyDeliveryReschedulePopup.getOrderItemsCount()
                                .should.eventually.to.be.equal(
                                    2,
                                    'Товаров из заказа два'
                                );

                            await this.verifyDeliveryReschedulePopup.getNthOrderItemCount(1, true)
                                .should.eventually.to.be.equal(
                                    8,
                                    'У второго товара отображается бейдж с количеством 8'
                                );
                        },
                    }),

                    'по клику на кнопку закрытия попап закрывается, агитация завершается': makeCase({
                        async test() {
                            await this.verifyDeliveryReschedulePopup.clickAcceptButton();

                            await this.verifyDeliveryReschedulePopup.clickHideButton();

                            await this.verifyDeliveryReschedulePopup.root.isExisting().catch(() => false)
                                .should.eventually.to.be.equal(
                                    false,
                                    'Попап об изменении сроков доставки не отображается'
                                );
                        },
                    }),

                    'при нажатии на кнопку чата агитация завершается, появляется чат арбитража': makeCase({
                        async test() {
                            await this.verifyDeliveryReschedulePopup.clickAcceptButton();

                            await this.verifyDeliveryReschedulePopup.clickChatButton();

                            await this.verifyDeliveryReschedulePopup.root.isExisting().catch(() => false)
                                .should.eventually.to.be.equal(
                                    false,
                                    'Попап об изменении сроков доставки не отображается'
                                );
                        },
                    }),
                },

                'при неподтверждении': {
                    'появляется окно с информацией о результате, отправляется запрос на завершение агитации': makeCase({
                        async test() {
                            await checkAgitationCompleted.call(this, () =>
                                this.verifyDeliveryReschedulePopup.clickDeclineButton()
                            );

                            await this.verifyDeliveryReschedulePopup.areResultsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран результатов отображается'
                                );

                            await this.verifyDeliveryReschedulePopup.areOrderItemsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Товары из заказа отображаются'
                                );

                            await this.verifyDeliveryReschedulePopup.getOrderItemsCount()
                                .should.eventually.to.be.equal(
                                    2,
                                    'Товаров из заказа два'
                                );

                            await this.verifyDeliveryReschedulePopup.getNthOrderItemCount(1, true)
                                .should.eventually.to.be.equal(
                                    8,
                                    'У второго товара отображается бейдж с количеством 8'
                                );
                        },
                    }),

                    'отправляется запрос на получение старых дат доставки': makeCase({
                        test() {
                            return this.browser.allure.runStep(
                                'Проверяем наличие запроса за датами доставки',
                                () => this.browser.yaWaitKadavrLogByBackendMethod(
                                    'Checkouter', 'getOrderEvents',
                                    () => this.verifyDeliveryReschedulePopup.clickDeclineButton()
                                )
                            );
                        },
                    }),

                    'по клику на кнопку закрытия попап закрывается, агитация завершается': makeCase({
                        async test() {
                            await this.verifyDeliveryReschedulePopup.clickDeclineButton();

                            await this.verifyDeliveryReschedulePopup.clickHideButton();

                            await this.verifyDeliveryReschedulePopup.root.isExisting().catch(() => false)
                                .should.eventually.to.be.equal(
                                    false,
                                    'Попап об изменении сроков доставки не отображается'
                                );
                        },
                    }),

                    'при нажатии на кнопку чата агитация завершается, появляется чат арбитража': makeCase({
                        async test() {
                            await this.verifyDeliveryReschedulePopup.clickDeclineButton();

                            await this.verifyDeliveryReschedulePopup.clickChatButton();

                            await this.verifyDeliveryReschedulePopup.root.isExisting().catch(() => false)
                                .should.eventually.to.be.equal(
                                    false,
                                    'Попап об изменении сроков доставки не отображается'
                                );
                        },
                    }),

                    'создается тикет': makeCase({
                        async test() {
                            await checkTicketCreated.call(this, () =>
                                this.verifyDeliveryReschedulePopup.clickDeclineButton()
                            );
                        },
                    }),
                },

                'по клику на крестик попап закрывается': makeCase({
                    async test() {
                        await this.verifyDeliveryReschedulePopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап подтверждения отмены отображается'
                            );

                        await checkAgitationCompleted.call(this,
                            () => this.verifyDeliveryReschedulePopup.clickCloser()
                        );

                        return this.verifyDeliveryReschedulePopup.root.isExisting().catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Попап подтверждения отмены не отображается'
                            );
                    },
                }),
            },
        },
    },
});

async function checkAgitationCompleted(action) {
    const log = await this.browser.yaWaitKadavrLogByBackendMethods(
        'PersAuthor', 'stopAgitationByIdAndUserId',
        action
    );

    return this.browser.allure.runStep(
        'Проверяем наличие запроса за завершением агитации',
        () => log.should.have.lengthOf(
            1,
            'В логе кадавра один запрос за завершением агитации'
        )
    );
}

async function checkTicketCreated(action) {
    const log = await this.browser.yaWaitKadavrLogByBackendMethods(
        'Startrek', 'createTicket',
        action,
        10000
    );

    return this.browser.allure.runStep(
        'Проверяем наличие запроса создания тикета',
        () => log.should.have.lengthOf(
            1,
            'В логе кадавра один запрос создания тикета'
        )
    );
}

async function prepareStateAndNavigate() {
    const {browser} = this;
    const orderId = generateRandomId();

    await browser.yaScenario(
        this,
        prepareOrder,
        {
            region: this.params.region,
            orders: [{
                orderId,
                items: [{
                    wareMd5: kettle.offerMock.wareId,
                    buyerPrice: 200,
                    count: 5,
                }, {
                    wareMd5: sock.offerMock.wareId,
                    buyerPrice: 200,
                    count: 8,
                }],
                deliveryType: 'DELIVERY',
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
            status: 'PROCESSING',
        }
    );

    const orderConsultations = [
        {
            orderNumber: orderId,
            chatId: '0/25/fb1cf15e-4b77-42b6-8808-3cd521b788c2',
            consultationStatus: 'DIRECT_CONVERSATION',
            conversationStatus: 'WAITING_FOR_CLIENT',
        },
    ];

    await this.browser.yaScenario(
        this,
        mockOrderConsultationsState,
        {orderConsultations}
    );

    await this.browser.setState('Checkouter.collections.events', {
        [orderId]: [{
            orderBefore: {
                delivery: {
                    dates: {
                        fromDate: '12-01-2021',
                        toDate: '21-01-2021',
                    },
                },
            },
        }],
    });

    const reportState = mergeState([
        createOffer(kettle.offerMock, kettle.offerMock.wareId),
        createOffer(sock.offerMock, sock.offerMock.wareId),
        {
            data: {
                search: {
                    results: [
                        kettle.offerMock.wareId,
                        sock.offerMock.wareId,
                    ].map(id => ({schema: 'offer', id})),
                    totalOffers: 2,
                    total: 2,
                },
            },
        },
    ]);
    await this.browser.yaScenario(this, setReportState, {state: reportState});

    await this.browser.yaScenario(this, prepareNotificationAgitation, {
        agitations: [{
            type: ORDER_AGITATION_TYPE.ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY,
            entityId: orderId,
        }],
    });


    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId, {mock: 1});
        await this.verifyDeliveryReschedulePopup.waitForVisibleRoot();
    }

    return orderId;
}
