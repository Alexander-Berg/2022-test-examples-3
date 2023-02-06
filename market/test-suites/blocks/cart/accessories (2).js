import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';

import {buildGoalName} from '@self/root/src/spec/utils/metrika';

import ReadySnippetCartButton from '@self/root/src/components/SnippetCartButton/components/ReadyCartButton/__pageObject';
import CounterCartButton from '@self/root/src/components/CounterCartButton/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import CommonlyPurchased from '@self/root/src/widgets/content/RootScrollBox/__pageObject/ScrollBoxes/InjectionCommonlyPurchased';
import Snippet from '@self/root/src/components/Snippet/__pageObject';

import * as largeCargoType from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import * as televisor from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as dresser from '@self/root/src/spec/hermione/kadavr-mock/report/dresser';
import * as accessories from '@self/root/src/spec/hermione/kadavr-mock/report/with-accessories';
import {createReportStateByEntities} from '@self/root/src/spec/utils/kadavr';
import * as vitaminsLowCost from '@self/root/src/spec/hermione/kadavr-mock/report/vitaminsLowCost';
import * as fuzzyFilters from '@self/root/src/spec/hermione/kadavr-mock/report/fuzzyFilters';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

const reportState = createReportStateByEntities([
    {
        sku: kettle.skuMock,
        offer: kettle.offerMock,
    },
    {
        sku: vitaminsLowCost.skuMock,
        offer: vitaminsLowCost.offerMock,
    },
    {
        product: televisor.productMock,
        offer: televisor.offerMock,
    },
    {
        product: fuzzyFilters.productMock,
    },
    {
        product: accessories.product,
        offer: accessories.offer,
    },
    {
        product: largeCargoType.productMock,
        offer: largeCargoType.offerMock,
    },
    {
        product: dresser.productMock,
        offer: dresser.offerMock,
    },
]);

const ROOT_ZONE = 'CART-PAGE';

export default makeSuite('Аксессуары', {
    feature: 'Аксессуары',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    orderTotal: () => this.createPageObject(OrderTotal),
                });
            },
        },
        makeSuite('Карусель "Не забыть купить"', {
            story: {
                async beforeEach() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-49185');
                    // eslint-disable-next-line no-unreachable
                    this.setPageObjects({
                        section: () => this.createPageObject(CommonlyPurchased),
                        secondSnippet: () => this.createPageObject(Snippet, {
                            parent: this.section,
                            root: Snippet.getSnippetByIndex(1),
                        }),
                        readyCartButton: () => this.createPageObject(ReadySnippetCartButton, {
                            parent: this.secondSnippet,
                        }),
                        counterCartButton: () => this.createPageObject(CounterCartButton, {
                            parent: this.secondSnippet,
                        }),
                    });
                    // eslint-disable-next-line no-unreachable
                    const carts = [
                        buildCheckouterBucket({
                            items: [{
                                skuMock: kettle.skuMock,
                                offerMock: kettle.offerMock,
                                count: 1,
                            }],
                        }),
                    ];
                    // eslint-disable-next-line no-unreachable
                    const state = await this.browser.yaScenario(
                        this,
                        prepareMultiCartState,
                        carts,
                        {existingReportState: reportState}
                    );
                    // eslint-disable-next-line no-unreachable
                    await this.browser.yaScenario(
                        this,
                        'cart.prepareCartPageBySkuId',
                        {
                            region: this.params.region,
                            items: [{
                                skuId: kettle.skuMock.id,
                                offerId: kettle.offerMock.wareId,
                            }],
                            count: 1,
                            reportSkus: state.reportSkus,
                        }
                    );
                    // eslint-disable-next-line no-unreachable
                    return this.browser.yaSlowlyScroll(CommonlyPurchased.root);
                },

                'Виджет отображается': makeCase({
                    id: 'bluemarket-2753',
                    issue: 'BLUEMARKET-6665',
                    test() {
                        return this.section.isVisible();
                    },
                }),

                'Заголовок секции правильный': makeCase({
                    id: 'bluemarket-2753',
                    issue: 'BLUEMARKET-6665',
                    test() {
                        const title = 'Не забыть купить';

                        return this.section.getTitleText()
                            .should
                            .eventually
                            .be
                            .equal(
                                title,
                                `Заголовок должен быть равен ${title}`
                            );
                    },
                }),

                'Кнопки "В корзину" отрабатывают корректно': makeCase({
                    id: 'bluemarket-2753',
                    issue: 'BLUEMARKET-6665',
                    test() {
                        return scrollBoxButtonsTest.call(this);
                    },
                }),

                'При клике на кнопку "В корзину" в метрику прилетает событие': makeCase({
                    id: 'bluemarket-2862',
                    issue: 'BLUEMARKET-6781',
                    async test() {
                        await this.readyCartButton.click();

                        return this.browser.yaScenario(
                            this,
                            'metrika.addToCart',
                            {
                                goalName: buildGoalName(ROOT_ZONE, 'SCROLL-BOX', 'SNIPPET'),
                                cartEntity: 'product',
                                payloadSchema: {
                                    garsons: Array.of({
                                        id: 'CommonlyPurchasedProducts',
                                    }),
                                    id: String,
                                    skuId: String,
                                },
                            }
                        );
                    },
                }),
            },
        })
    ),
});

async function scrollBoxButtonsTest() {
    let counterCartButtonVisible;
    let readyButtonVisible;

    // const cartTotalPrice = await this.orderTotal.getPriceValue();

    counterCartButtonVisible = await this.counterCartButton.isExisting().catch(() => false);
    await this.expect(counterCartButtonVisible)
        .to
        .be
        .equal(false, 'Кнопка со счетчиком товаров в корзине не видна');

    readyButtonVisible = await this.readyCartButton.isVisible();
    await this.expect(readyButtonVisible)
        .to
        .be
        .equal(true, 'Кнопка "В корзину" видна');

    await this.readyCartButton.click();

    counterCartButtonVisible = await this.counterCartButton.isVisible();
    await this.expect(counterCartButtonVisible)
        .to
        .be
        .equal(true, 'Кнопка со счетчиком товаров в корзине видна');

    readyButtonVisible = await this.readyCartButton.isVisible();
    await this.expect(readyButtonVisible)
        .to
        .be
        .equal(false, 'Кнопка "В корзину" не видна');

    // Наверное автор хотел что-то проверить, но не ясно что именно
    // await this.browser.yaGetLastKadavrLogByBackendMethod('Carter', 'addItem');

    // TODO: BLUEMARKET-6434
    // const newCartTotalPrice = await this.orderTotal.getPriceValue();

    // await this.expect(newCartTotalPrice)
    //     .to.be.not.equal(cartTotalPrice, 'Новая цена корзины не равна старой');
}
