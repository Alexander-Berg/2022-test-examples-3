/* eslint-disable global-require */

import {within} from '@testing-library/dom';

import {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';
import {mountSearchWidgets, SEARCH_ROOT_WIDGET_PATH} from '@self/root/src/widgets/content/search/__spec__/mocks/mountSearchWidgets';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
import {
    mockBasicSearchFunctionality,
    mockNoopBackendFunctionality,
    mockSearchFunctionality,
    mockSearchViewType,
    makeContext,
} from '@self/root/src/widgets/content/search/__spec__/mocks';
import SearchSnippetCell from '@self/root/market/src/components/Search/Snippet/Cell/__pageObject';
import {VIEWTYPE} from '@self/root/src/resources/report/params/viewtype';
import {parseTestingUrl} from '@self/root/src/helpers/testament/url';
import CartButton from '@self/root/market/src/components/CartButton/__pageObject';
import ProductWarning from '@self/root/market/src/components/ProductWarning/__pageObject/index.desktop';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

beforeAll(async () => {
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            asLibrary: true,
        },
    });

    mandrelLayer = mirror.getLayer('mandrel');
    jestLayer = mirror.getLayer('jest');
    apiaryLayer = mirror.getLayer('apiary');
    kadavrLayer = mirror.getLayer('kadavr');
});

async function mountSerp() {
    return apiaryLayer.mountWidget(
        SEARCH_ROOT_WIDGET_PATH,
        mountSearchWidgets([
            {widgetName: 'SearchSerp', props: {emptyType: 'withReset'}},
            {widgetName: 'PromoHintTooltip', props: {}},
        ])
    );
}

function findFirstSnippet(container) {
    return container.querySelector('article:nth-of-type(1)');
}

describe('Search: Результаты поиска', () => {
    describe('Выдача без категории', () => {
        const pageId = PAGE_IDS_COMMON.SEARCH;
        const initialParams = {}; // Выдача должна уметь рисоваться без любых параметров, это важно

        describe('Белый сниппет', () => {
            beforeEach(async () => {
                await mockBasicSearchFunctionality({jestLayer}, pageId, initialParams);
                await mockNoopBackendFunctionality({jestLayer, kadavrLayer});
                await makeContext({kadavrLayer, mandrelLayer, jestLayer}, initialParams);
            });

            describe.each([
                ['Гридовая сетка', VIEWTYPE.GRID],
                ['Листовой список', VIEWTYPE.LIST],
            ])('%s', (_, viewtype) => {
                let container;
                let firstSnippet;

                beforeEach(async () => {
                    await mockSearchViewType({jestLayer}, viewtype);
                });

                describe('Сниппет карточки', () => {
                    describe('По умолчанию', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {commo: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        // MARKETFRONT-96354
                        // Search: Результаты поиска Выдача без категории Белый сниппет Гридовая сетка Сниппет карточки По умолчанию должен отображаться
                        // Падение 27.66%
                        // eslint-disable-next-line jest/no-disabled-tests
                        test.skip('должен отображаться', () => {
                            expect(firstSnippet.querySelector(SearchSnippetCell.root)).toBeVisible();
                        });

                        it('изображение отображается', () => {
                            const snippetImage = within(firstSnippet).getByRole('img');

                            expect(snippetImage).toBeVisible();
                        });

                        test('должен иметь заголовок', () => {
                            expect(firstSnippet.querySelector(SearchSnippetCell.title).textContent)
                                .toEqual('Сетевое зарядное устройство COMMO Compact charger 20W Dual Type-C, Белый');
                        });

                        test('должен вести на КМ по ссылке из заголовка', () => {
                            const href = firstSnippet.querySelector(SearchSnippetCell.titleLink).href;

                            expect(parseTestingUrl(href)).toEqual({
                                pageId: 'market:offer',
                                searchParams: {
                                    cpc: 'hzlHgw6EVjiKSWrmUQ-X__15gvShhA_e3y3NXUV5WhB9TM4ZYwB1LY0IvkHrxH1rQUBqwcjIOw0da2rgf3h7REHddqAiMN1XF70NqxA8M3XHv6lJOjEjcy0WaExzJNd2ikRuFMio9ayt2gWu4YbXD8b5TOprSySDKvc_QF63SJzh6WLo7Ql9OUUvL6s5urS4',
                                    hid: '91503',
                                    hyperid: '1663274219',
                                    lr: '213',
                                    modelid: '1663274219',
                                    nid: '26894030',
                                    offerId: 'v3F9A5qsN2JL9OsoBWpQIA',
                                    'show-uid': '16578111841531030258700002',
                                    text: 'commo compact charger',
                                },
                            });
                        });

                        test('должен отображаться скидочный бейдж', () => {
                            expect(firstSnippet.querySelector(SearchSnippetCell.discountBadge).textContent)
                                .toEqual('Скидка:‒7%');
                        });

                        test('должен иметь цену товара в указанном формате', () => {
                            expect(firstSnippet.querySelector(SearchSnippetCell.mainPrice).textContent).toEqual(`1 290${NBSP}₽`);
                        });

                        test('должен иметь кнопку добавления в корзину', () => {
                            expect(firstSnippet.querySelector(CartButton.root).textContent).toMatch(/В.корзину/);
                        });
                    });

                    describe('Наличие флага "Новинка"', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {novelty: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        test('бейдж должен присутствовать', () => {
                            expect(firstSnippet.querySelector(SearchSnippetCell.hypeBadge)).toBeVisible();
                        });
                    });

                    describe('Наличие предзаказа', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {preorderProduct: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        test('текст на кнопке должен быть корректным', () => {
                            expect(firstSnippet.querySelector(CartButton.root).textContent).toMatch(/Предзаказ/);
                        });
                    });

                    describe('Наличие дисклеймера', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {disclaimerProduct: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        test('дисклеймер должен отображаться', () => {
                            expect(firstSnippet.querySelector(`${ProductWarning.root}:nth-of-type(1)`)).toBeVisible();
                        });

                        test('должен иметь правильный тип', () => {
                            expect(firstSnippet.querySelector(`${ProductWarning.root}:nth-of-type(1)`).getAttribute('data-auto')).toEqual('medicine');
                        });

                        test('должен содержать правильный текст', () => {
                            expect(firstSnippet.querySelector(`${ProductWarning.root}:nth-of-type(1)`).textContent).toEqual('Есть противопоказания, посоветуйтесь с врачом');
                        });
                    });

                    describe('Наличие промокода', () => {
                        let promoCodeElement;

                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {promoCodeProduct: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);

                            promoCodeElement = firstSnippet.querySelector('div[data-autotest-id="info"]');
                        });

                        test('должен отображаться', () => {
                            expect(firstSnippet).toBeVisible();
                        });

                        test('должен содержать текст "Ещё -10% по промокоду"', () => {
                            expect(promoCodeElement.textContent).toEqual(`Ещё −10% по${NBSP}промокоду`);
                        });
                    });

                    describe('Информация о других предложениях', () => {
                        const productOptions = {
                            product: {
                                nid: '345',
                                offers: {count: 3},
                                showUid: 'a',
                                navnodes: [{id: 26894030}],
                            },
                        };
                        describe('при больше чем одном предложении', () => {
                            beforeEach(async () => {
                                await mockSearchFunctionality({kadavrLayer}, {product: true}, productOptions);

                                container = (await mountSerp()).container;
                                firstSnippet = findFirstSnippet(container);
                            });

                            it('содержит текст "X предложений", если их больше 1', () => {
                                const moreOffersLinkContainer = within(firstSnippet).getByRole('link', {name: /3 предложения от/i});

                                expect(moreOffersLinkContainer).toBeVisible();
                            });

                            it('должны вести на КМ по ссылке', () => {
                                const moreOffersLinkContainer = within(firstSnippet).getByRole('link', {name: /3 предложения от/i});

                                const moreOffersLink = moreOffersLinkContainer.getAttribute('href');

                                expect(parseTestingUrl(moreOffersLink)).toEqual({
                                    pageId: 'market:product',
                                    searchParams: {
                                        context: 'search',
                                        nid: 26894030,
                                        productId: '123',
                                        'show-uid': 'a',
                                        slug: 'product',
                                        track: 'srchbtn',
                                    },
                                });
                            });
                        });
                        describe('при одном предложении', () => {
                            beforeEach(async () => {
                                await mockSearchFunctionality({kadavrLayer}, {product: true}, {
                                    product: {
                                        ...productOptions.product,
                                        offers: {
                                            count: 1,
                                        },
                                    },
                                });

                                container = (await mountSerp()).container;
                                firstSnippet = findFirstSnippet(container);
                            });

                            it('не содержит текст "X предложений"', async () => {
                                expect(within(firstSnippet).queryByRole('link', {name: /1 предложение от/i})).toBeNull();
                            });
                        });
                    });

                    describe('Спонсорский товар', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {commo: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        it('должен содержать плашку с информацией о спонсорском товаре', async () => {
                            const promoDisclaimer = within(firstSnippet).getByText(/спонсорский товар/i);

                            expect(promoDisclaimer).toBeVisible();
                        });
                    });
                });

                describe('Сниппет оффера', () => {
                    describe('Наличие скидки', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {discountOffer: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        test('бейдж должен присутствовать', () => {
                            expect(firstSnippet.querySelector(SearchSnippetCell.discountBadge)).toBeVisible();
                        });
                    });

                    describe('Наличие предзаказа', () => {
                        beforeEach(async () => {
                            await mockSearchFunctionality({kadavrLayer}, {preorderOffer: true});

                            container = (await mountSerp()).container;
                            firstSnippet = findFirstSnippet(container);
                        });

                        test('текст на кнопке должен быть корректным', () => {
                            expect(firstSnippet.querySelector(CartButton.root).textContent).toMatch(/Предзаказ/);
                        });

                        test('ссылка на кнопке должна быть корректной', () => {
                            const href = firstSnippet.querySelector(CartButton.root).href;
                            expect(parseTestingUrl(href)).toEqual({
                                pageId: 'market:my-checkout',
                                searchParams: {
                                    cartItems: [expect.stringContaining('Yf2Psrdybfmvolweb7JdqA')],
                                    cartItemsSchema: 'offerId:count:feeShow:bundleId:isPrimaryInBundle:services',
                                },
                            });
                        });
                    });
                });
            });
        });
    });
});
