import {
    makeCase,
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {pathOrUnsafe} from 'ambar';
import assert from 'assert';

import Text from '@self/root/src/uikit/components/Text/__pageObject';
import Similar from '@self/root/src/widgets/content/Similar/__pageObject';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_EDIT_REQUEST_TYPE} from '@self/root/src/entities/orderEditRequest/constants';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import * as tv from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {
    mergeState,
    createSku,
    createProductForSku,
    createOfferForSku,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import createSimilarPopupCase from '../similarPopup';


const ITEM_PRICE = 888;
const REMOVED_ITEMS_TEXT_POSTPAID =
    `К сожалению, этого товара не оказалось на складе — без него заказ стоит на ${ITEM_PRICE} ₽ дешевле`;
const REMOVED_ITEMS_TEXT_PREPAID =
    `К сожалению, этого товара не оказалось на складе — за него вернём ${ITEM_PRICE} ₽ в течение 3 дней`;


/**
 * Тесты на удаленный из заказа товар.
 */
module.exports = makeSuite('Удаление товара из заказа', {
    feature: 'Удаление товара из заказа',
    issue: 'BLUEMARKET-13937',
    environment: 'kadavr',

    story: mergeSuites({
        async beforeEach() {
            assert(
                this.orderItems,
                'PageObject orderItems (OrderItems/MyOrderItems) must be defined in order to run this suite'
            );
            assert(
                this.changedOrderItem,
                'PageObject changedOrderItem (OrderItem/MyOrderItem) must be defined in order to run this suite'
            );

            await this.setPageObjects({
                changedOrderItemsText: () => this.createPageObject(
                    Text,
                    {
                        parent: this.orderItems.changedItemsBlock,
                    }
                ),
                similarPopup: () => this.createPageObject(Similar),
            });
        },
        'Предоплатный заказ': createRemovedItemsCase({isPrepaidOrder: true}),
        'Постоплатный заказ': createRemovedItemsCase({isPrepaidOrder: false}),
    },
    prepareSuite(createSimilarPopupCase({
        feature: 'Удаление товара из заказа',
    }), {
        hooks: {
            async beforeEach() {
                await prepareStateAndNavigate.call(this, {isPrepaidOrder: true, withOnlyItem: true});

                await this.changedOrderItem.clickSimilarButton();
                return this.similarPopup.waitForVisibleRoot();
            },
        },
    })),
});

export function createRemovedItemsCase({isPrepaidOrder}) {
    return {
        beforeEach() {
            return prepareStateAndNavigate.call(this, {isPrepaidOrder});
        },
        'В блоке удаленных товаров': {
            'должны отображаться уведомление и удаленный товар': makeCase({
                id: 'bluemarket-3665',

                async test() {
                    const removedItemsHeaderText = isPrepaidOrder
                        ? REMOVED_ITEMS_TEXT_PREPAID
                        : REMOVED_ITEMS_TEXT_POSTPAID;

                    await this.changedOrderItemsText.getText()
                        .should.eventually.to.be.equal(
                            removedItemsHeaderText,
                            `Отображается текст "${removedItemsHeaderText}"`
                        );

                    await this.orderItems.hasVisibleChangedItems()
                        .should.eventually.to.be.equal(true, 'Удаленный товар отображается');

                    const changedItems = await this.orderItems.changedItems;

                    changedItems.value.length
                        .should.equal(1, 'Удаленный товар единственный');

                    await this.changedOrderItem.hasSimilarButton()
                        .should.eventually.to.be.equal(true, 'Отображается кнопка похожих товаров');
                },
            }),
            'по клику на кнопку "Похожие товары" появляется попап похожих товаров': makeCase({
                id: 'bluemarket-3670',

                async test() {
                    await this.changedOrderItem.clickSimilarButton();
                    await this.similarPopup.waitForVisibleRoot();

                    await this.similarPopup.isPopupVisible()
                        .should.eventually.to.be.equal(true, 'Попап похожих товаров отображается');
                },
            }),
        },
    };
}

async function prepareStateAndNavigate({isPrepaidOrder}) {
    const {browser} = this;
    const payment = isPrepaidOrder
        ? {
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
        }
        : {
            paymentType: 'POSTPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
        };
    const prepareOrderResponse = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            region: this.params.region,
            orders: [{
                items: [{
                    skuId: kettle.skuMock.id,
                    buyerPrice: ITEM_PRICE,
                    count: 10,
                    deltaCount: -1,
                }],
                editRequests: [{type: ORDER_EDIT_REQUEST_TYPE.ITEMS_REMOVAL}],
                deliveryType: 'DELIVERY',
            }],
            ...payment,
            status: 'PROCESSING',
        }
    );

    const reportState = mergeState([
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

    await this.browser.yaScenario(this, setReportState, {state: reportState});

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        const orderId = pathOrUnsafe(null, ['orders', 0, 'id'], prepareOrderResponse);

        return this.browser.yaOpenPage(this.params.pageId, {orderId});
    }

    await this.browser.yaOpenPage(this.params.pageId);

    return this.orderItems.waitForVisibleRoot();
}
