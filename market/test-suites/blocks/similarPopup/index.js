/* eslint-disable */

import assert from 'assert';
import {
    makeCase,
    makeSuite,
} from 'ginny';
import {isNil, identity} from 'ambar';

import Similar from '@self/root/src/widgets/content/Similar/__pageObject';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
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

import {HAS_HEADER} from './constants';


export default function createSimilarPopupTestSuite({
    feature,
    pageId,
    itemsCount = 1,

    shouldMockOwnData = false,
    alternativeOfferExistence = new Array(itemsCount).fill(true),
    alternativeOfferSupplierEquality = new Array(itemsCount).fill(true),
    shouldTestAlternativeOffer = true,
    shouldTestSimilarItems = true,

    testPalmIds = {
        popupClose: undefined,
        navigateToCart: undefined,
        visible: undefined,
        multiStepSwitch: undefined,
        multiOfferAddToCart: undefined,
        multiOfferNavigateToSku: undefined,
        similarSnippetAddToCart: undefined,
        similarSnippetNavigateToSku: undefined,
    },
}) {
    return makeSuite('Попап похожих товаров.', {
        environment: 'kadavr',
        feature,
        issue: 'MARKETFRONT-16668',
        id: 'marketfront-4964',

        story: {
            async beforeEach() {
                assert(
                    feature,
                    'feature must be defined'
                );

                assert(
                    itemsCount >= 1,
                    'itemsCount must be higher or equal 1'
                );

                assert(
                    alternativeOfferExistence.length === itemsCount,
                    'alternativeOfferExistence must equal itemsCount'
                );

                assert(
                    alternativeOfferSupplierEquality.length === itemsCount,
                    'alternativeOfferSupplierEquality must equal itemsCount'
                );

                if (shouldTestAlternativeOffer) {
                    assert(
                        alternativeOfferExistence.some(identity),
                        'at least one item must have alternativeOffer when shouldTestAlternativeOffer is true'
                    );
                }

                await this.setPageObjects({
                    similar: () => this.createPageObject(Similar),
                });

                if (shouldMockOwnData) {
                    await prepareState.call(this);
                }
            },
            'В попапе похожих товаров': {
                ...createVisibilityTestCases({
                    itemsCount,
                    alternativeOfferExistence,
                    alternativeOfferSupplierEquality,
                    shouldTestAlternativeOffer,
                    testPalmIds,
                }),
                ...createClosingCase({testPalmIds}),
                ...createCartNavigationCase({itemsCount, pageId, testPalmIds}),
            },
            ...createAlternativeOfferCases({
                shouldTestAlternativeOffer,
                alternativeOfferExistence,
                testPalmIds,
            }),
            ...createSimilarItemsCases({shouldTestSimilarItems, testPalmIds}),
        },
    });
}

function createClosingCase({
    testPalmIds: {popupClose},
}) {
    return {
        'по клику на крестик попап должен закрыться': makeCase({
            id: popupClose,

            async test() {
                await this.similar.isPopupVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Попап похожих товаров отображается'
                    );

                await this.similar.clickCloser();

                return this.similar.root.isExisting().catch(() => false)
                    .should.eventually.to.be.equal(
                        false,
                        'Попап похожих товаров не отображается'
                    );
            },
        }),
    };
}

function createCartNavigationCase({
    itemsCount,
    pageId,
    testPalmIds: {
        navigateToCart,
    },
}) {
    if (pageId === PAGE_IDS_COMMON.CART) {
        return {};
    }

    return {
        'по клику на кнопку перехода должен происходить переход в корзину': makeCase({
            id: navigateToCart,

            async test() {
                for (let i = 0; i < itemsCount - 1; ++i) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.similar.clickNextButton();
                }

                await testNextButtonText.call(this);

                const url = await this.browser.yaWaitForChangeUrl(
                    () => this.similar.clickNextButton()
                );

                await this.allure.runStep(
                    'Проверяем, что перешли в корзину',
                    () => this.expect(url).to.be.link({
                        pathname: '^/my/cart',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    })
                );
            },
        }),
    };
}

function createVisibilityTestCases({
    itemsCount,
    alternativeOfferExistence,
    alternativeOfferSupplierEquality,
    shouldTestAlternativeOffer,
    testPalmIds: {
        visible,
        multiStepSwitch,
    },
}) {
    const severalItems = itemsCount > 1 ? {
        'для попапа с несколькими товарами должен работать последовательный переход': makeCase({
            id: multiStepSwitch,

            async test() {
                for (let i = 0; i < itemsCount; ++i) {
                    // eslint-disable-next-line no-await-in-loop
                    await testVisibility.call(this, {
                        itemsCount,
                        currentItemIndex: i,
                        shouldTestHeader: HAS_HEADER,
                        shouldTestHeaderText: false,
                        isMeantToHaveAlternativeOffer: shouldTestAlternativeOffer
                            && alternativeOfferExistence[i],
                        isAlternativeOfferSupplierEqual: shouldTestAlternativeOffer
                            && alternativeOfferSupplierEquality[i],
                    });

                    if (i + 1 !== itemsCount) {
                        // eslint-disable-next-line no-await-in-loop
                        await this.similar.clickNextButton();
                    }
                }
            },
        }),
    } : {};

    return {
        'основные элементы попапа должны отображаться': makeCase({
            id: visible,

            async test() {
                await testVisibility.call(this, {
                    itemsCount,
                    currentItemIndex: 0,
                    shouldTestHeader: HAS_HEADER,
                    shouldTestHeaderText: HAS_HEADER,
                    isMeantToHaveAlternativeOffer: shouldTestAlternativeOffer
                        && alternativeOfferExistence[0],
                    isAlternativeOfferSupplierEqual: shouldTestAlternativeOffer
                        && alternativeOfferSupplierEquality[0],
                });
            },
        }),
        ...severalItems,
    };
}

function testVisibility({
    itemsCount,
    currentItemIndex,
    shouldTestHeader = true,
    shouldTestHeaderText,
    isMeantToHaveAlternativeOffer,
    isAlternativeOfferSupplierEqual,
}) {
    return this.allure.runStep(
        'Проверяем видимость элементов',
        async () => {
            await this.similar.isPopupVisible()
                .should.eventually.to.be.equal(
                    true,
                    'Попап похожих товаров отображается'
                );

            if (itemsCount > 1) {
                await testCounter.call(this, {current: currentItemIndex, total: itemsCount});
            }

            if (this.params.pageId !== PAGE_IDS_COMMON.CART || itemsCount !== currentItemIndex + 1) {
                await testNextButtonText.call(this, {current: currentItemIndex, total: itemsCount});
            }

            if (shouldTestHeader) {
                await this.similar.isHeaderVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Шапка попапа похожих товаров отображается'
                    );
            }

            if (shouldTestHeaderText) {
                await this.similar.getHeaderText()
                    .should.eventually.to.be.equal(
                        'Товар закончился, но есть похожие — выбирайте',
                        'Текст попапа верный'
                    );
            }

            if (isMeantToHaveAlternativeOffer) {
                await this.similar.isAlternativeOfferVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Блок мультиоффера отображается'
                    );

                if (!isNil(isAlternativeOfferSupplierEqual)) {
                    const description = await this.similar.getAlternativeOfferDescriptionText();
                    const requiredDescription = isAlternativeOfferSupplierEqual
                        ? 'Другие условия'
                        : 'У другого продавца на Маркете';

                    this.expect(description).to.equal(
                        requiredDescription,
                        'Комментарий к мультиофферу верный'
                    );
                }
            }

            return this.similar.areSimilarItemsVisible()
                .should.eventually.to.be.equal(
                    true,
                    'Похожие товары видны'
                );
        }
    );
}

function testNextButtonText({current = 0, total = 1} = {}) {
    const requiredText = current + 1 === total
        ? 'Перейти в корзину'
        : 'К следующему товару';

    return this.similar.getNextButtonText()
        .should.eventually.to.be.equal(
            requiredText,
            `Кнопка перехода содержит верный текст "${requiredText}"`
        );
}

function testCounter({current, total}) {
    return this.allure.runStep(
        'Проверяем каунтер',
        async () => {
            await this.similar.isCounterVisible()
                .should.eventually.to.be.equal(
                    true,
                    'Каунтер отображается'
                );

            const {
                current: currentActual,
                total: totalActual,
            } = await this.similar.getCounterData();

            this.expect(currentActual).to.equal(
                current + 1,
                `Текущий номер товара для которого смотрим похожие — ${current + 1}`
            );

            return this.expect(totalActual).to.equal(
                total,
                `Всего товаров, для которых смотрим похожие — ${total}`
            );
        }
    );
}

function createAlternativeOfferCases({
    shouldTestAlternativeOffer,
    alternativeOfferExistence,
    testPalmIds: {
        multiOfferAddToCart,
        multiOfferNavigateToSku,
    },
}) {
    if (!shouldTestAlternativeOffer) {
        return {};
    }

    return {
        'В блоке мультиоффера': {
            async beforeEach() {
                const itemWithAlternativeOfferNumber = alternativeOfferExistence.indexOf(true);

                for (let i = 0; i < itemWithAlternativeOfferNumber; ++i) {
                    // eslint-disable-next-line no-await-in-loop
                    await this.similar.clickNextButton();
                }
            },
            'клик по кнопке добавления в корзину должен добавлять оффер в корзину': makeCase({
                id: multiOfferAddToCart,

                async test() {
                    await this.similar.isAlternativeOfferVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Блок мультиоффера отображается'
                        );

                    return this.browser.yaWaitKadavrLogByBackendMethods(
                        'Carter', 'addItem',
                        () => this.similar.clickAlternativeOfferCartButton()
                    )
                        .should.eventually.to.have.lengthOf(
                            1,
                            'Ушел один запрос за добавлением товара в корзину'
                        );
                },
            }),
            'клик по ссылке оффера открывает карточку товара': makeCase({
                id: multiOfferNavigateToSku,

                async test() {
                    await this.similar.isAlternativeOfferVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Блок мультиоффера отображается'
                        );

                    return testSkuNavigation.call(this,
                        'Кликаем по ссылке оффера и переключаемся на новую вкладку',
                        () => this.similar.clickAlternativeOfferLink()
                    );
                },
            }),
        },
    };
}

function createSimilarItemsCases({
    shouldTestSimilarItems,
    testPalmIds: {
        similarSnippetAddToCart,
        similarSnippetNavigateToSku,
    },
}) {
    if (!shouldTestSimilarItems) {
        return {};
    }

    return {
        'В блоке похожих товаров': {
            'клик по кнопке добавления в корзину должен добавлять оффер в корзину': makeCase({
                id: similarSnippetAddToCart,

                async test() {
                    await this.similar.getNthSnippet(0).isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Первый сниппет отображается'
                        );

                    return this.browser.yaWaitKadavrLogByBackendMethods(
                        'Carter', 'addItem',
                        () => this.similar.clinkNthSnippetCartButton(0)
                    )
                        .should.eventually.to.have.lengthOf(
                            1,
                            'Ушел один запрос за добавлением товара в корзину'
                        );
                },
            }),
            'клик по сниппету открывает карточку товара': makeCase({
                id: similarSnippetNavigateToSku,

                async test() {
                    await this.similar.areSimilarItemsVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Блок похожих товаров отображается'
                        );

                    return testSkuNavigation.call(this,
                        'Кликаем по ссылке сниппета и переключаемся на новую вкладку',
                        () => this.similar.clinkNthSnippetLink(0)
                    );
                },
            }),
        },
    };
}

async function testSkuNavigation(message, action) {
    await this.allure.runStep(
        message,
        async () => {
            const tabIds = await this.browser.getTabIds();

            await action();
            const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});

            await this.browser.switchTab(newTabId);
        });

    return this.allure.runStep(
        'Проверяем, что перешли на карточку товара',
        async () => {
            const url = await this.browser.getUrl();

            return this.expect(url).to.be.link(
                {pathname: /product(--|\/)[a-z0-9-_]+\/\d+$/},
                {mode: 'match', skipProtocol: true, skipHostname: true}
            );
        }
    );
}

function prepareState() {
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

    return this.browser.yaScenario(this, setReportState, {state: reportState});
}
