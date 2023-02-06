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

import VerifyCancellationPopup from '@self/root/src/widgets/content/orderIssues/VerifyCancellationPopup/__pageObject';
import {SHOW_PRODUCTS_IN_APOLOGIES} from '@self/root/src/widgets/content/orderIssues/VerifyCancellationPopup/constants';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_AGITATION_TYPE} from '@self/root/src/constants/orderAgitation';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareNotificationAgitation} from '@self/root/src/spec/hermione/scenarios/persAuthorResource';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';


export default makeSuite('Попап подтверждения отмены пользователем со стороны продавца.', {
    environment: 'kadavr',
    feature: 'Подтверждение отмены DSBS',
    issue: 'MARKETFRONT-36470',
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
                verifyCancellationPopup: () => this.createPageObject(VerifyCancellationPopup),
            });
        },
        'Видимость элементов попапа.': {
            async beforeEach() {
                await prepareStateAndNavigate.call(this);

                return this.verifyCancellationPopup.waitForVisibleRoot();
            },
            'В попапе подтверждения отмены должны отображаться товары из заказа': makeCase({
                async test() {
                    await this.verifyCancellationPopup.isPopupVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап подтверждения отмены отображается'
                        );

                    await this.verifyCancellationPopup.areOrderItemsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Товары из заказа отображаются'
                        );

                    await this.verifyCancellationPopup.isHeaderVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Заголовок отображается'
                        );

                    await this.verifyCancellationPopup.isDescriptionVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Описание отображается'
                        );

                    await this.verifyCancellationPopup.getOrderItemsCount()
                        .should.eventually.to.be.equal(
                            2,
                            'Товаров из заказа два'
                        );

                    await this.verifyCancellationPopup.areButtonsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кнопки подтверждения и неподтверждения отображаются'
                        );

                    await this.verifyCancellationPopup.getNthOrderItemCount(1)
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

                return this.verifyCancellationPopup.waitForVisibleRoot();
            },
            'В попапе подтверждения отмены': {
                'по клику на кнопку подтверждения попап закрывается, агитация завершается': makeCase({
                    async test() {
                        assert(
                            this.yaTestData.currentOrderId,
                            'this.yaTestData.currentOrderId must be defined'
                        );

                        await this.verifyCancellationPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап подтверждения отмены отображается'
                            );

                        await this.verifyCancellationPopup.areButtonsVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопки подтверждения и неподтверждения отображаются'
                            );

                        await checkAgitationCompleted.call(this, () =>
                            this.verifyCancellationPopup.clickAcceptButton()
                        );

                        return this.verifyCancellationPopup.root.isExisting().catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Попап подтверждения отмены не отображается'
                            );
                    },
                }),
                'при неподтверждении агитация завершается, появляется экран извинений': makeCase({
                    async test() {
                        assert(
                            this.yaTestData.currentOrderId,
                            'this.yaTestData.currentOrderId must be defined'
                        );

                        await this.verifyCancellationPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап подтверждения отмены отображается'
                            );

                        await this.verifyCancellationPopup.areButtonsVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопки подтверждения и неподтверждения отображаются'
                            );

                        await checkAgitationCompleted.call(this, () =>
                            this.verifyCancellationPopup.clickDeclineButton()
                        );

                        await this.verifyCancellationPopup.areApologiesVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Экран извинений отображается'
                            );

                        if (SHOW_PRODUCTS_IN_APOLOGIES) {
                            await this.verifyCancellationPopup.areOrderItemsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Товары из заказа отображаются'
                                );

                            await this.verifyCancellationPopup.getOrderItemsCount()
                                .should.eventually.to.be.equal(
                                    2,
                                    'Товаров из заказа два'
                                );

                            await this.verifyCancellationPopup.getNthOrderItemCount(1)
                                .should.eventually.to.be.equal(
                                    8,
                                    'У второго товара отображается бейдж с количеством 8'
                                );
                        }
                    },
                }),
                'при неподтверждении создается тикет': makeCase({
                    async test() {
                        assert(
                            this.yaTestData.currentOrderId,
                            'this.yaTestData.currentOrderId must be defined'
                        );

                        await this.verifyCancellationPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап подтверждения отмены отображается'
                            );

                        await this.verifyCancellationPopup.areButtonsVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопки подтверждения и неподтверждения отображаются'
                            );

                        await checkTicketCreated.call(this, () =>
                            this.verifyCancellationPopup.clickDeclineButton()
                        );

                        await this.verifyCancellationPopup.areApologiesVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Экран извинений отображается'
                            );
                    },
                }),
                'по клику на крестик попап закрывается': makeCase({
                    async test() {
                        await this.verifyCancellationPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап подтверждения отмены отображается'
                            );

                        await checkAgitationCompleted.call(this,
                            () => this.verifyCancellationPopup.clickCloser()
                        );

                        return this.verifyCancellationPopup.root.isExisting().catch(() => false)
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
            type: ORDER_AGITATION_TYPE.ORDER_CANCELLED_BY_USER_EXTERNALLY,
            entityId: orderId,
        }],
    });


    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId, {mock: 1});
        await this.verifyCancellationPopup.waitForVisibleRoot();
    }

    return orderId;
}
