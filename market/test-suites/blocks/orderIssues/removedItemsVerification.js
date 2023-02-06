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

import RemovedItemsVerificationPopup
    from '@self/root/src/widgets/content/orderIssues/RemovedItemsVerificationPopup/__pageObject';
import Similar from '@self/root/src/widgets/content/Similar/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_AGITATION_TYPE} from '@self/root/src/constants/orderAgitation';
import {ORDER_EDIT_REQUEST_TYPE} from '@self/root/src/entities/orderEditRequest/constants';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareNotificationAgitation} from '@self/root/src/spec/hermione/scenarios/persAuthorResource';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';


export default makeSuite('Попап подтверждения удаления пользователем товаров из заказа.', {
    environment: 'kadavr',
    feature: 'Удаление товара из заказа DSBS',
    issue: 'MARKETFRONT-38055',
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
                similarPopup: () => this.createPageObject(Similar),
                removedItemsVerificationPopup: () => this.createPageObject(RemovedItemsVerificationPopup),
            });
        },
        'Видимость элементов попапа.': {
            async beforeEach() {
                await prepareStateAndNavigate.call(this);

                return this.removedItemsVerificationPopup.waitForVisibleRoot();
            },
            'В попапе подтверждения удаления пользователем товаров должны отображаться удаленные товары': makeCase({
                async test() {
                    await this.removedItemsVerificationPopup.isPopupVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап подтверждения удаления пользователем товаров отображается'
                        );

                    await this.removedItemsVerificationPopup.areRemovedOrderItemsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Удаленные товары из заказа отображаются'
                        );

                    await this.removedItemsVerificationPopup.isHeaderVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Заголовок отображается'
                        );

                    await this.removedItemsVerificationPopup.isDescriptionVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Описание отображается'
                        );

                    await this.removedItemsVerificationPopup.getRemovedOrderItemsCount()
                        .should.eventually.to.be.equal(
                            2,
                            'Удаленных товаров два'
                        );

                    await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кнопки подтверждения и неподтверждения отображаются'
                        );

                    await this.removedItemsVerificationPopup.getNthOrderItemCount(1)
                        .should.eventually.to.be.equal(
                            2,
                            'У второго товара отображается бейдж с количеством 2'
                        );
                },
            }),
        },
        'Функциональность попапа.': {
            async beforeEach() {
                this.yaTestData = this.yaTestData || {};
                this.yaTestData.currentOrderId = await prepareStateAndNavigate.call(this);

                return this.removedItemsVerificationPopup.waitForVisibleRoot();
            },
            'В попапе подтверждения удаления': {
                'при подтверждении удаления': {
                    'агитация завершается, открывается экран результатов': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.isPopupVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Попап подтверждения удаления отображается'
                                );

                            await this.removedItemsVerificationPopup.isQuestionnaireVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран с вопросом отображается'
                                );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkAgitationCompleted.call(this, () =>
                                this.removedItemsVerificationPopup.clickAcceptButton()
                            );

                            await this.removedItemsVerificationPopup.root.isExisting()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Попап подтверждения удаления отображается'
                                );

                            await this.removedItemsVerificationPopup.areResultsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран результатов отображается'
                                );

                            await this.removedItemsVerificationPopup.areRemovedOrderItemsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Удаленные товары из заказа отображаются'
                                );

                            await this.removedItemsVerificationPopup.getRemovedOrderItemsCount()
                                .should.eventually.to.be.equal(
                                    2,
                                    'Удаленных товаров два'
                                );

                            await this.removedItemsVerificationPopup.getNthOrderItemCount(1)
                                .should.eventually.to.be.equal(
                                    2,
                                    'У второго товара отображается бейдж с количеством 2'
                                );
                        },
                    }),
                    'открывается попап похожих товаров на экране результатов по нажатию кнопки': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkAgitationCompleted.call(this, () =>
                                this.removedItemsVerificationPopup.clickAcceptButton()
                            );

                            await this.removedItemsVerificationPopup.areResultsButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки финального экрана отображаются'
                                );

                            await this.removedItemsVerificationPopup.clickSimilarButton();

                            await this.similarPopup.waitForVisibleRoot();

                            await this.similarPopup.isPopupVisible()
                                .should.eventually.to.be.equal(true, 'Попап похожих товаров отображается');
                        },
                    }),
                    'по нажатию кнопки открывается страница заказа': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkAgitationCompleted.call(this, () =>
                                this.removedItemsVerificationPopup.clickAcceptButton()
                            );

                            await this.removedItemsVerificationPopup.areResultsButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки финального экрана отображаются'
                                );

                            if (this.params.pageId !== PAGE_IDS_COMMON.ORDER) {
                                const url = await this.browser.yaWaitForChangeUrl(
                                    () => this.removedItemsVerificationPopup.clickNavigateToOrderButton()
                                );

                                return this.allure.runStep(
                                    'Проверяем, что перешли на страницу заказа',
                                    () => this.expect(url).to.be.link({
                                        pathname: `^/my/order/${this.yaTestData.currentOrderId}`,
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                        skipQuery: true,
                                    })
                                );
                            }
                        },
                    }),
                },
                'при неподтверждении удаления': {
                    'агитация завершается, появляется экран результатов': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.isPopupVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Попап подтверждения удаления отображается'
                                );

                            await this.removedItemsVerificationPopup.isQuestionnaireVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран с вопросом отображается'
                                );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkAgitationCompleted.call(this, () =>
                                this.removedItemsVerificationPopup.clickDeclineButton()
                            );

                            await this.removedItemsVerificationPopup.areResultsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран результатов отображается'
                                );

                            await this.removedItemsVerificationPopup.areRemovedOrderItemsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Удаленные товары из заказа отображаются'
                                );

                            await this.removedItemsVerificationPopup.getRemovedOrderItemsCount()
                                .should.eventually.to.be.equal(
                                    2,
                                    'Удаленных товаров два'
                                );

                            await this.removedItemsVerificationPopup.getNthOrderItemCount(1)
                                .should.eventually.to.be.equal(
                                    2,
                                    'У второго товара отображается бейдж с количеством 2'
                                );
                        },
                    }),
                    'создается тикет в стартрек': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.isPopupVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Попап подтверждения удаления отображается'
                                );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkTicketCreated.call(this, () =>
                                this.removedItemsVerificationPopup.clickDeclineButton()
                            );

                            await this.removedItemsVerificationPopup.areResultsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Экран результатов отображается'
                                );
                        },
                    }),
                    'открывается попап похожих товаров на экране результатов по нажатию кнопки': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkAgitationCompleted.call(this, () =>
                                this.removedItemsVerificationPopup.clickDeclineButton()
                            );

                            await this.removedItemsVerificationPopup.areResultsButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки финального экрана отображаются'
                                );

                            await this.removedItemsVerificationPopup.clickSimilarButton();

                            await this.similarPopup.waitForVisibleRoot();

                            await this.similarPopup.isPopupVisible()
                                .should.eventually.to.be.equal(true, 'Попап похожих товаров отображается');
                        },
                    }),
                    'по нажатию кнопки открывается страница заказа': makeCase({
                        async test() {
                            assert(
                                this.yaTestData.currentOrderId,
                                'this.yaTestData.currentOrderId must be defined'
                            );

                            await this.removedItemsVerificationPopup.areQuestionnaireButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки подтверждения и неподтверждения отображаются'
                                );

                            await checkAgitationCompleted.call(this, () =>
                                this.removedItemsVerificationPopup.clickDeclineButton()
                            );

                            await this.removedItemsVerificationPopup.areResultsButtonsVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопки финального экрана отображаются'
                                );

                            if (this.params.pageId !== PAGE_IDS_COMMON.ORDER) {
                                const url = await this.browser.yaWaitForChangeUrl(
                                    () => this.removedItemsVerificationPopup.clickNavigateToOrderButton()
                                );

                                return this.allure.runStep(
                                    'Проверяем, что перешли на страницу заказа',
                                    () => this.expect(url).to.be.link({
                                        pathname: `^/my/order/${this.yaTestData.currentOrderId}`,
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                        skipQuery: true,
                                    })
                                );
                            }
                        },
                    }),
                },
                'по клику на крестик попап закрывается, агитация завершается': makeCase({
                    async test() {
                        await this.removedItemsVerificationPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап подтверждения удаления отображается'
                            );

                        await checkAgitationCompleted.call(this,
                            () => this.removedItemsVerificationPopup.clickCloser()
                        );

                        return this.removedItemsVerificationPopup.root.isExisting().catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Попап подтверждения удаления не отображается'
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
                    skuId: null,
                    buyerPrice: 200,
                    count: 10,
                    deltaCount: -1,
                }, {
                    wareMd5: sock.offerMock.wareId,
                    skuId: null,
                    buyerPrice: 200,
                    count: 10,
                    deltaCount: -2,
                }],
                editRequests: [{type: ORDER_EDIT_REQUEST_TYPE.ITEMS_REMOVAL}],
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
            type: ORDER_AGITATION_TYPE.ORDER_ITEM_REMOVED_BY_USER_EXTERNALLY,
            entityId: orderId,
        }],
    });


    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId, {mock: 1});
        await this.removedItemsVerificationPopup.waitForVisibleRoot();
    }

    return orderId;
}
