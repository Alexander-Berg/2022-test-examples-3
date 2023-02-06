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

import CancellationRejectionPopup from '@self/root/src/widgets/content/orderIssues/CancellationRejectionPopup/__pageObject';
import YandexMessenger from '@self/root/src/widgets/core/YandexMessenger/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_AGITATION_TYPE} from '@self/root/src/constants/orderAgitation';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareNotificationAgitation} from '@self/root/src/spec/hermione/scenarios/persAuthorResource';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {mockOrderConsultationsState} from '@self/root/src/spec/hermione/scenarios/orderConsultation';


export default makeSuite('Попап отказа продавца в отмене со стороны пользователя', {
    environment: 'kadavr',
    feature: 'Запрос на отмену продавцу DSBS',
    issue: 'MARKETFRONT-38028',
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
                cancellationRejectionPopup: () => this.createPageObject(CancellationRejectionPopup),
                yandexMessenger: () => this.createPageObject(YandexMessenger),
            });
        },
        'Видимость элементов попапа.': {
            async beforeEach() {
                await prepareStateAndNavigate.call(this);

                return this.cancellationRejectionPopup.waitForVisibleRoot();
            },
            'В попапе отказа в отмене должны отображаться товары из заказа': makeCase({
                async test() {
                    await this.cancellationRejectionPopup.isPopupVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап отказа в отмене отображается'
                        );

                    await this.cancellationRejectionPopup.areOrderItemsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Товары из заказа отображаются'
                        );

                    await this.cancellationRejectionPopup.isHeaderVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Заголовок отображается'
                        );

                    await this.cancellationRejectionPopup.isDescriptionVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Описание отображается'
                        );

                    await this.cancellationRejectionPopup.getOrderItemsCount()
                        .should.eventually.to.be.equal(
                            2,
                            'Товаров из заказа два'
                        );

                    await this.cancellationRejectionPopup.areButtonsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кнопки подтверждения и неподтверждения отображаются'
                        );

                    await this.cancellationRejectionPopup.getNthOrderItemCount(1)
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

                return this.cancellationRejectionPopup.waitForVisibleRoot();
            },
            'В попапе отказа в отмене': {
                'по клику на кнопку закрытия попап закрывается, агитация завершается': makeCase({
                    async test() {
                        assert(
                            this.yaTestData.currentOrderId,
                            'this.yaTestData.currentOrderId must be defined'
                        );

                        await this.cancellationRejectionPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап отказа в отмене отображается'
                            );

                        await this.cancellationRejectionPopup.areButtonsVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопки выхода и открытия арбитража отображаются'
                            );

                        await checkAgitationCompleted.call(this, () =>
                            this.cancellationRejectionPopup.clickHideButton()
                        );

                        return this.cancellationRejectionPopup.root.isExisting().catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Попап отказа в отмене не отображается'
                            );
                    },
                }),
                'по клику на крестик попап закрывается': makeCase({
                    async test() {
                        await this.cancellationRejectionPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап отказа в отмене отображается'
                            );

                        await checkAgitationCompleted.call(this,
                            () => this.cancellationRejectionPopup.clickCloser()
                        );

                        return this.cancellationRejectionPopup.root.isExisting().catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Попап отказа в отмене не отображается'
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

async function prepareStateAndNavigate() {
    const {browser} = this;
    const orderId = generateRandomId();

    await this.browser.yaSetCookie({
        name: 'autotest_hide_chat',
        value: '0',
        path: '/',
    });

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
            type: ORDER_AGITATION_TYPE.ORDER_CANCELLATION_REJECTED_BY_SHOP,
            entityId: orderId,
        }],
    });


    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId, {mock: 1});
        await this.cancellationRejectionPopup.waitForVisibleRoot();
    }

    return orderId;
}
