import assert from 'assert';
import {
    makeCase,
    makeSuite,
    prepareSuite,
} from 'ginny';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

import Similar from '@self/root/src/widgets/content/Similar/__pageObject';
import DeletedOrderItemsPopup from '@self/root/src/widgets/content/orderIssues/DeletedOrderItemsPopup/__pageObject';
import createSimilarPopupCase from '@self/root/src/spec/hermione/test-suites/blocks/similarPopup';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_EDIT_REQUEST_TYPE} from '@self/root/src/entities/orderEditRequest/constants';
import {ORDER_AGITATION_TYPE} from '@self/root/src/constants/orderAgitation';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import * as tv from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareNotificationAgitation} from '@self/root/src/spec/hermione/scenarios/persAuthorResource';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {
    mergeState,
    createSku,
    createOffer,
    createProductForSku,
    createOfferForSku,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';


const ITEM_PRICE = 222;
const REMOVED_ITEMS_TEXT_POSTPAID =
    `К сожалению, этих товаров не оказалось на складе — без них заказ стоит на ${ITEM_PRICE * 3} ₽ дешевле`;
const REMOVED_ITEMS_TEXT_PREPAID =
    `К сожалению, этих товаров не оказалось на складе — за них вернём ${ITEM_PRICE * 3} ₽ в течение 3 дней`;

export default makeSuite('Удаление товара из заказа. Нотификация.', {
    environment: 'kadavr',
    feature: 'Удаление товара из заказа',
    issue: 'BLUEMARKET-15045',
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
                deletedOrderItemsPopup: () => this.createPageObject(DeletedOrderItemsPopup),
            });
        },
        'Предоплатный заказ.': createDeletedOrderItemsPopupDisplayCase({isPrepaidOrder: true}),
        'Постоплатный заказ.': createDeletedOrderItemsPopupDisplayCase({isPrepaidOrder: false}),
        'DSBS.': {
            'Предоплатный заказ.': createDeletedOrderItemsPopupDisplayCase({isPrepaidOrder: true, isDsbs: true}),
            'Постоплатный заказ.': createDeletedOrderItemsPopupDisplayCase({isPrepaidOrder: false, isDsbs: true}),
        },
        'Функциональность попапа.': {
            async beforeEach() {
                this.yaTestData = this.yaTestData || {};
                this.yaTestData.currentOrderId = await prepareStateAndNavigate.call(this, {isPrepaidOrder: true});

                return this.deletedOrderItemsPopup.waitForVisibleRoot();
            },
            'В попапе удаленных товаров': {
                'по клику на кнопку "Перейти к заказу" происходит переход на страницу заказа': makeCase({
                    id: 'bluemarket-3663',

                    async test() {
                        assert(
                            this.yaTestData.currentOrderId,
                            'this.yaTestData.currentOrderId must be defined'
                        );

                        await this.deletedOrderItemsPopup.areButtonsVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопки перехода в попап похожих товаров и в мои заказы отображаются'
                            );

                        let url;

                        if (this.params.pageId !== PAGE_IDS_COMMON.ORDER) {
                            await checkAgitationCompleted.call(this, async () => {
                                url = await this.browser.yaWaitForChangeUrl(
                                    () => this.deletedOrderItemsPopup.clickOrderButton()
                                );
                            });

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
                'по клику на крестик попап закрывается': makeCase({
                    id: 'bluemarket-3664',

                    async test() {
                        await this.deletedOrderItemsPopup.isPopupVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Попап удаленных товаров из заказа отображается'
                            );

                        await checkAgitationCompleted.call(this,
                            () => this.deletedOrderItemsPopup.clickCloser()
                        );

                        return this.deletedOrderItemsPopup.root.isExisting().catch(() => false)
                            .should.eventually.to.be.equal(
                                false,
                                'Попап удаленных товаров из заказа не отображается'
                            );
                    },
                }),
                'по клику на кнопку "Похожие товары" появляется попап похожих товаров': makeCase({
                    id: 'bluemarket-3662',

                    async test() {
                        await checkAgitationCompleted.call(this,
                            () => this.deletedOrderItemsPopup.clickSimilarButton()
                        );

                        await this.similarPopup.waitForVisibleRoot();

                        await this.similarPopup.isPopupVisible()
                            .should.eventually.to.be.equal(true, 'Попап похожих товаров отображается');
                    },
                }),
            },
        },
        'Удален один товар': prepareSuite(createSimilarPopupCase({
            feature: 'Удаление товара из заказа',
            shouldTestAlternativeOffer: false,
            testPalmIds: {
                visible: 'bluemarket-3672',
                similarSnippetAddToCart: 'bluemarket-3674',
            },
        }), {
            hooks: {
                async beforeEach() {
                    await prepareStateAndNavigate.call(this, {isPrepaidOrder: true, withOnlyItem: true});

                    await this.deletedOrderItemsPopup.waitForVisibleRoot();

                    return this.deletedOrderItemsPopup.clickSimilarButton();
                },
            },
        }),
        'Удалено два товара.': prepareSuite(createSimilarPopupCase({
            feature: 'Удаление товара из заказа',
            itemsCount: 2,
            testPalmIds: {
                popupClose: 'bluemarket-3669',
                navigateToCart: 'bluemarket-3668',
                visible: 'bluemarket-3673',
                multiStepSwitch: 'bluemarket-3667',
                similarSnippetAddToCart: 'bluemarket-3674',
            },
        }), {
            hooks: {
                async beforeEach() {
                    await prepareStateAndNavigate.call(this, {isPrepaidOrder: true});

                    await this.deletedOrderItemsPopup.waitForVisibleRoot();

                    return this.deletedOrderItemsPopup.clickSimilarButton();
                },
            },
        }),
    },
});

function createDeletedOrderItemsPopupDisplayCase({isPrepaidOrder, isDsbs}) {
    let testPalmId;

    if (!isDsbs) {
        testPalmId = isPrepaidOrder
            ? 'bluemarket-3671'
            : 'bluemarket-3661';
    }

    return {
        async beforeEach() {
            await prepareStateAndNavigate.call(this, {isPrepaidOrder, isDsbs});

            return this.deletedOrderItemsPopup.waitForVisibleRoot();
        },
        'В попапе удаленных товаров': {
            'должны отображаться уведомление и удаленные товары': makeCase({
                id: testPalmId,

                async test() {
                    const removedItemsHeaderText = isPrepaidOrder
                        ? REMOVED_ITEMS_TEXT_PREPAID
                        : REMOVED_ITEMS_TEXT_POSTPAID;

                    await this.deletedOrderItemsPopup.isPopupVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Попап удаленных товаров из заказа отображается'
                        );

                    await this.deletedOrderItemsPopup.isDescriptionVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Уведомление об удаленных товарах отображается'
                        );

                    if (!isDsbs) {
                        await this.deletedOrderItemsPopup.getDescriptionText()
                            .should.eventually.to.be.equal(
                                removedItemsHeaderText,
                                `Отображается уведомление "${removedItemsHeaderText}"`
                            );
                    }

                    await this.deletedOrderItemsPopup.areOrderItemsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Картинки удаленных товаров отображаются'
                        );

                    const changedOrderItems = await this.deletedOrderItemsPopup.orderItems;

                    changedOrderItems.value.length
                        .should.equal(2, 'Удаленных товаров два');

                    await this.deletedOrderItemsPopup.areButtonsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кнопки перехода в попап похожих товаров и в мои заказы отображаются'
                        );

                    return this.deletedOrderItemsPopup.getNthOrderItemCount(1)
                        .should.eventually.to.be.equal(
                            2,
                            'У второго товара отображается бейдж с количеством 2'
                        );
                },
            }),
        },
    };
}

async function checkAgitationCompleted(action) {
    const log = await this.browser.yaWaitKadavrLogByBackendMethods(
        'PersAuthor', 'stopAgitationByIdAndUserId',
        action
    );

    return this.browser.allure.runStep(
        'Проверяем наличие запроса за удалением агитации',
        () => log.should.have.lengthOf(
            1,
            'В логе кадавра один запрос за удалением агитации'
        )
    );
}

async function prepareStateAndNavigate({isPrepaidOrder = true, withOnlyItem, isDsbs = false}) {
    const {browser} = this;
    const orderId = generateRandomId();
    const payment = isPrepaidOrder
        ? {
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
        }
        : {
            paymentType: 'POSTPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
        };

    await browser.yaScenario(
        this,
        prepareOrder,
        {
            region: this.params.region,
            orders: [{
                orderId,
                items: [{
                    skuId: isDsbs ? null : kettle.skuMock.id,
                    wareMd5: isDsbs ? kettle.offerMock.wareId : undefined,
                    buyerPrice: ITEM_PRICE,
                    count: 10,
                    deltaCount: -1,
                }, (
                    !withOnlyItem && {
                        skuId: isDsbs ? null : sock.skuMock.id,
                        wareMd5: isDsbs ? sock.offerMock.wareId : undefined,
                        buyerPrice: ITEM_PRICE,
                        count: 10,
                        deltaCount: -2,
                    }
                )].filter(Boolean),
                editRequests: [{type: ORDER_EDIT_REQUEST_TYPE.ITEMS_REMOVAL}],
                deliveryType: 'DELIVERY',
                ...(isDsbs ? {
                    delivery: {
                        deliveryPartnerType: 'SHOP',
                    },
                } : {}),
            }],
            ...payment,
            status: 'PROCESSING',
            ...(isDsbs ? {
                fulfilment: false,
                rgb: 'WHITE',
            } : {}),
        }
    );
    let reportState;

    if (isDsbs) {
        reportState = mergeState([
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
    } else {
        reportState = mergeState([
            createSku(kettle.skuMock, kettle.skuMock.id),
            createProductForSku(kettle.productMock, kettle.skuMock.id, kettle.productMock.id),
            createOfferForSku(kettle.offerMock, kettle.skuMock.id, kettle.offerMock.wareId),
            createOfferForProduct(kettle.offerMock, kettle.productMock.id, kettle.offerMock.wareId),

            createSku(sock.skuMock, sock.skuMock.id),
            createProductForSku(sock.productMock, sock.skuMock.id, sock.productMock.id),
            createOfferForSku(sock.offerMock, sock.skuMock.id, sock.offerMock.wareId),
            createOfferForProduct(sock.offerMock, sock.productMock.id, sock.offerMock.wareId),

            createSku(tv.skuMock, tv.skuMock.id),
            createProductForSku(tv.productMock, tv.skuMock.id, tv.productMock.id),
            {
                data: {
                    search: {
                        results: [
                            kettle.productMock.id,
                            sock.productMock.id,
                        ].map(id => ({schema: 'product', id})),
                        totalOffers: 2,
                        total: 2,
                    },
                },
            },
        ]);
    }

    await this.browser.yaScenario(this, setReportState, {state: reportState});

    await this.browser.yaScenario(this, prepareNotificationAgitation, {
        agitations: [{
            type: ORDER_AGITATION_TYPE.ORDER_ITEM_REMOVAL,
            entityId: orderId,
        }],
    });

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        await this.browser.yaOpenPage(this.params.pageId, {orderId});
    } else {
        await this.browser.yaOpenPage(this.params.pageId, {mock: 1});
        await this.deletedOrderItemsPopup.waitForVisibleRoot();
    }

    return orderId;
}
