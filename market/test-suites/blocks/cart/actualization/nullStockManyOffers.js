import {any} from 'ambar';

import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as tv from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {buildCheckouterBucket, buildCheckouterBucketLabel} from '@self/root/src/spec/utils/checkouter';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import CartOfferWishlistDisclaimer
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferWishlistDisclaimer/__pageObject';
import SoldOutOverlay
    from '@self/root/src/widgets/content/cart/CartList/containers/CartOfferPictureContainer/__pageObject';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';

const commonSuite = makeSuite('Проверка корзины при обнуления стока', {
    story: {
        beforeEach() {
            this.setPageObjects({
                soldOutOverlay: () => this.createPageObject(
                    SoldOutOverlay,
                    {parent: this.unavailableCartOffer}
                ),
            });
        },

        'Заголовок корзины содержит текст "Корзина"': makeCase({
            test() {
                return this.cartHeader.getTitleText()
                    .should.eventually.to.be.include(
                        CART_TITLE,
                        `Заголовок корзины должен содержать текст "${CART_TITLE}"`
                    );
            },
        }),

        'Бейдж "Разобрали" отображается': makeCase({
            async test() {
                return this.soldOutOverlay.isSoldOutVisible()
                    .should.eventually.to.be.equal(true, 'Бейдж "разобрали" должен отображаться');
            },
        }),

        'Сумма и количество товаров в саммари отображается корректно': makeCase({
            async test() {
                await this.orderTotal.getItemsCount()
                    .should.eventually.to.be.equal(
                        1,
                        'Количество товаров в саммари должно быть равно 1'
                    );

                return this.orderTotal.getItemsValue()
                    .should.eventually.to.be.equal(
                        Number(kettle.offerMock.prices.value),
                        `Цена товаров равна ${kettle.offerMock.prices.value}`
                    );
            },
        }),
    },
});

export default makeSuite('Несколько офферов в корзине.', {
    id: 'bluemarket-2769',
    issue: 'BLUEMARKET-6864',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cartOfferWishlistDisclaimer: () => this.createPageObject(
                        CartOfferWishlistDisclaimer,
                        {parent: this.unavailableCartOffer}
                    ),
                });
            },
        },

        prepareSuite(commonSuite, {
            suiteName: 'Оффер кончилися по стокам.',
            params: {
                items: [{
                    skuId: kettle.skuMock.id,
                    offerId: kettle.offerMock.wareId,
                }, {
                    skuId: tv.skuMock.id,
                    offerId: tv.offerMock.wareId,
                    /**
                     * При загрузке страницы отрисуется состояние из картера - все в наличии
                     * После актуализации из чекаутера придет count = 0, появится надпись "Разобрали"
                     */
                    count: 0,
                }],
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {
                        mocks: [kettle, tv],
                    });

                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: this.params.region});
                    return this.browser.yaScenario(this, waitForCartActualization);
                },
            },
        }),

        prepareSuite(commonSuite, {
            suiteName: 'Оффера уже нет в репорте.',
            params: {
                items: [{
                    skuId: kettle.skuMock.id,
                    offerId: kettle.offerMock.wareId,
                }, {
                    skuId: tv.skuMock.id,
                    offerId: tv.offerMock.wareId,
                    /**
                     * При загрузке страницы отрисуется состояние из картера - все в наличии
                     * После актуализации из чекаутера придет count = 0, появится надпись "Разобрали"
                     */
                    count: 0,
                    isExpired: true,
                    isSkippedInReport: true,
                }],
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {
                        mocks: [kettle, tv],
                    });

                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: this.params.region});
                    return this.browser.yaScenario(this, waitForCartActualization);
                },
            },
        })
    ),
});

function prepareState({mocks = []}) {
    const carts = [];

    const cart = buildCheckouterBucket({
        label: buildCheckouterBucketLabel(
            this.params.items
                .filter(item => !item.isExpired)
                .map((item, i) => mocks[i].offerMock.wareId), 2),
        warehouseId: 2,
        items: this.params.items.map((item, i) => {
            if (item.isExpired) {
                return null;
            }

            return {
                ...item,
                skuMock: mocks[i].skuMock,
                offerMock: mocks[i].offerMock,
            };
        }).filter(Boolean),
        region: this.params.region,
    });

    carts.push(cart);

    if (any(item => item.isExpired, this.params.items)) {
        const expipedCart = buildCheckouterBucket({
            shopId: 0,
            warehouseId: 0,
            items: this.params.items.map((item, i) => {
                if (!item.isExpired) {
                    return null;
                }

                return {
                    ...item,
                    skuMock: mocks[i].skuMock,
                    offerMock: mocks[i].offerMock,
                };
            }).filter(Boolean),
            region: this.params.region,
        });

        carts.push(expipedCart);
    }

    return this.browser.yaScenario(
        this,
        prepareMultiCartState,
        carts.filter(Boolean)
    );
}
