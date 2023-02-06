import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import * as tv from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';

import ExpiredCartOffer from '@self/root/src/widgets/content/cart/CartList/components/ExpiredCartOffer/__pageObject';
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import ReadyCartButton from '@self/root/src/components/SnippetCartButton/components/ReadyCartButton/__pageObject';

import {
    mergeState,
    createSku,
    createProductForSku,
    createOfferForSku,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartOfferDetails from '@self/root/src/widgets/content/cart/CartList/components/CartOfferDetails/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import {ACTUALIZATION_TIMEOUT} from '@self/root/src/spec/hermione/scenarios/checkout';

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

export default makeSuite('Разобранные товары.', {
    environment: 'kadavr',
    id: 'bluemarket-3473',
    issue: 'BLUEMARKET-11521',
    story: {
        async beforeEach() {
            this.setPageObjects({
                expiredCartOffer: () => this.createPageObject(ExpiredCartOffer),
                cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                firstCartItem: () => this.createPageObject(CartItem, {
                    parent: this.cartGroup,
                    root: this.cartItems.firstItem,
                }),
                firstOfferDetails: () => this.createPageObject(CartOfferDetails, {
                    parent: this.firstCartItem,
                }),
                secondCartItem: () => this.createPageObject(CartItem, {
                    parent: this.cartGroup,
                    root: this.cartItems.secondItem,
                }),
                secondOfferDetails: () => this.createPageObject(CartOfferDetails, {
                    parent: this.secondCartItem,
                }),
                popupModal: () => this.createPageObject(PopupBase),
                firstSnippet: () => this.createPageObject(Snippet, {
                    parent: this.popupModal,
                }),
                firstSnippetCartButton: () => this.createPageObject(ReadyCartButton, {
                    parent: this.firstSnippet,
                }),
                secondSnippet: () => this.createPageObject(Snippet, {
                    parent: this.popupModal,
                    root: Snippet.getSnippetByIndex(1),
                }),
                secondSnippetCartButton: () => this.createPageObject(ReadyCartButton, {
                    parent: this.secondSnippet,
                }),
            });

            const cart = buildCheckouterBucket({
                shopId: 0,
                warehouseId: 0,
                items: [{
                    skuId: tv.skuMock.id,
                    offerId: tv.offerMock.wareId,
                    count: 0,
                    isExpired: true,
                    isSkippedInReport: true,
                    skuMock: tv.skuMock,
                    offerMock: tv.offerMock,
                }],
                withoutDelivery: true,
                region: 213,
                deliveryOptions: [],
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [cart],
                {existingReportState: reportState}
            );

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
        },
        'Замена товара на похожие': makeCase({
            async test() {
                await this.firstOfferDetails.getTitle()
                    .should.eventually.to.be.equal(
                        tv.skuMock.titles.raw,
                        `Название разобранного товара в корзине должно быть "${tv.skuMock.titles.raw}"`
                    );
                await this.expiredCartOffer.isSimilarButtonVisible()
                    .should.eventually.to.be.equal(true, 'Кнопка "Похожие товары" должна быть видна');
                await this.expiredCartOffer.clickSimilarButton();
                await this.popupModal.waitForVisible();

                const carts = [
                    /**
                     * 2 одинаковых айтема - не ошибка, а немного магии и обман кадавра.
                     * 1-й айтем удалится после клика на кнопку, 2-й останется в стейте.
                     * А подкладывать данные в стейт после клика по кнопке уже поздно.
                     */
                    buildCheckouterBucket({
                        items: [{
                            skuId: kettle.skuMock.id,
                            offerId: kettle.offerMock.wareId,
                            count: 1,
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                        }, {
                            skuId: kettle.skuMock.id,
                            offerId: kettle.offerMock.wareId,
                            count: 1,
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                        }],
                        withoutDelivery: true,
                        region: 213,
                        deliveryOptions: [],
                    }),
                ];

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts,
                    {existingReportState: reportState}
                );

                await this.firstSnippetCartButton.click();

                await this.browser.allure.runStep('Ждем замены оффера',
                    () => this.browser.waitUntil(
                        () => this.firstOfferDetails.getTitle()
                            .should.eventually.to.be.equal(
                                kettle.skuMock.titles.raw,
                                `Название первого товара в корзине должно быть "${kettle.skuMock.titles.raw}"`
                            ),
                        ACTUALIZATION_TIMEOUT,
                        'Разобранный товар должен замениться на похожий'
                    )
                );

                await this.popupModal.waitForVisible();

                const carts2 = [
                    buildCheckouterBucket({
                        items: [{
                            skuId: kettle.skuMock.id,
                            offerId: kettle.offerMock.wareId,
                            count: 1,
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                        }, {
                            skuId: sock.skuMock.id,
                            offerId: sock.offerMock.wareId,
                            count: 1,
                            skuMock: sock.skuMock,
                            offerMock: sock.offerMock,
                        }],
                        withoutDelivery: true,
                        region: 213,
                        deliveryOptions: [],
                    }),
                ];

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts2,
                    {existingReportState: reportState}
                );

                await this.secondSnippetCartButton.click();
                await this.browser.allure.runStep('Ждем добавления оффера',
                    () => this.browser.waitUntil(
                        () => this.secondOfferDetails.getTitle()
                            .should.eventually.to.be.equal(
                                sock.skuMock.titles.raw,
                                `Название второго товара в корзине должно быть "${sock.skuMock.titles.raw}"`
                            ),
                        ACTUALIZATION_TIMEOUT,
                        'В корзину должен добавиться похожий товар'
                    )
                );
                await this.popupModal.waitForVisible();
                return this.browser.allure.runStep('Закрываем попап нажатием на крестик',
                    async () => {
                        await this.popupModal.cross.click();
                        return this.popupModal.waitForNonexisting();
                    }
                );
            },
        }),
    },
});
