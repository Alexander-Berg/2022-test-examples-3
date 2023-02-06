import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import SlugReviewProductSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/reviewProduct';
import SlugCheckLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/checkLink';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import RouterLink from '@self/platform/spec/page-objects/RouterLink';
import SubpageHeader from '@self/platform/spec/page-objects/SubpageHeader';

import {report, schema, publicId, wishlishItem} from './mocks';
import articlePageMock from './article.mock';
import collectionsPageMock from './collections.mock';
import brandPageMock from './brand.mock';
import franchisePageMock from './franchise.mock';

const urlParams = {
    productId: 1,
    slug: 'product',
};

const suiteParams = {
    entity: 'product',
};

export default makeSuite('Продукт', {
    story: mergeSuites(
        makeSuite('Страница КМ', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', report());
                        return this.browser.yaOpenPage('touch:product', urlParams)
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                makeSuite('Ссылка на страницу КМ с отзывами', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2406',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            selector: '[data-autotest-id="product-info-links"] [data-autotest-id="reviews"] a',
                        },
                    }),
                }),
                makeSuite('Ссылка на страницу КМ со всеми вопросами', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2408',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            selector: '[data-autotest-id="product-info-links"] [data-autotest-id="questions"]',
                        },
                    }),
                }),
                makeSuite('Ссылка на страницу КМ с характеристиками', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2409',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            selector: '[data-autotest-id="product-info-links"] [data-autotest-id="spec"] a',
                        },
                    }),
                })
            ),
        }),
        makeSuite('Страница характеристик КМ', {
            story: mergeSuites(
                makeSuite('Ссылка на страницу КМ с отзывами', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2406',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            // костыль, непонятно почему RouterLink не находится релесектором
                            selector: '[data-autotest-id="reviews"] a',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', report());
                                return this.browser.yaOpenPage('touch:product-spec', urlParams)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                prepareSuite(SlugReviewProductSuite, {
                    meta: {
                        id: 'm-touch-2706',
                        issue: 'MOBMARKET-9940',
                    },
                    pageObjects: {
                        newReviewLink() {
                            return this.createPageObject(RouterLink, {
                                root: `[data-autotest-id="reviews"] ${RouterLink.root}`,
                            });
                        },
                        subpageHeader() {
                            return this.createPageObject(SubpageHeader);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report({opinions: 0}));
                            return this.browser.yaOpenPage('touch:product-spec', urlParams)
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                })
            ),
        }),
        makeSuite('Страница конкретного вопроса о КМ', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', report());
                        return this.browser.yaOpenPage('touch:product-question', {
                            productId: urlParams.productId,
                            productSlug: urlParams.slug,
                            questionId: 1,
                            questionSlug: 'question',
                        })
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                makeSuite('Ссылка в шапке на страницу КМ со всеми вопросами', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2408',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            selector: '[data-autotest-id="title-card"] [data-autotest-id="questions"]',
                        },
                    }),
                }),
                makeSuite('Ссылка в подвале на страницу КМ со всеми вопросами', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2408',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            selector: '[data-autotest-id="ask-more-form-slot"] [data-autotest-id="questions"]',
                        },
                    }),
                })
            ),
        }),
        makeSuite('Страница отзывов о КМ', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', report());
                        return this.browser.yaOpenPage('touch:product-reviews', urlParams)
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                prepareSuite(SlugReviewProductSuite, {
                    meta: {
                        id: 'm-touch-2624',
                        issue: 'MOBMARKET-9940',
                    },
                    pageObjects: {
                        newReviewLink() {
                            return this.createPageObject(RouterLink, {
                                root: `[data-autotest-id="new-review"] ${RouterLink.root}`,
                            });
                        },
                        subpageHeader() {
                            return this.createPageObject(SubpageHeader);
                        },
                    },
                })
            ),
        }),
        makeSuite('Страница отзывов пользователя', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: schema.users,
                            modelOpinions: schema.modelOpinions,
                            gradesOpinions: schema.gradesOpinions,
                        });
                        await this.browser.setState('report', report());
                        return this.browser.yaOpenPage('touch:user-reviews', {publicId})
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                makeSuite('Ссылка «Ответить» в отзыве о продукта', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2639',
                            issue: 'MOBMARKET-9940',
                        },
                        params: {
                            ...suiteParams,
                            selector: '[data-autotest-id="reply"]',
                        },
                    }),
                })
            ),
        }),

        makeSuite('Проверка всех ссылок, которые ведут на КМ', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.params = {
                            ...suiteParams,
                            selector: `a[href^="/${suiteParams.entity}"]`,
                        };
                    },
                },
                makeSuite('На главной странице', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('touch:index')
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                                await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                                // Делаем долго, чтоб успели виджеты прогрузиться
                                // eslint-disable-next-line market/ginny/no-pause
                                await this.browser.pause(10000);
                            },
                        },
                    }),
                }),
                makeSuite('На странице департамента', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('touch:catalog', routes.catalog.electronics)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице выдачи', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('touch:list', routes.catalog.tile)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице выдачи + отзывы покупателей', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('touch:list', routes.catalog.reviewsHubCategory)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице статьи', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [articlePageMock]);
                                await this.browser.yaOpenPage('touch:articles', routes.articles.kofemolki)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице подборки', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [collectionsPageMock.Tarantino]);
                                await this.browser.setState('report', collectionsPageMock.report);
                                await this.browser.yaOpenPage('touch:collections', routes.collections)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице статьи Журнала', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage(
                                    'market:journal-article',
                                    routes.journalArticle.holodilnik
                                )
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице КМ', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('touch:product', routes.product.phone)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице бренда', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [brandPageMock.Tarantino]);
                                await this.browser.setState('report', brandPageMock.report);
                                await this.browser.yaOpenPage('touch:brands', routes.brand)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице франшизы', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [franchisePageMock.Tarantino]);
                                await this.browser.setState('report', franchisePageMock.report);
                                await this.browser.yaOpenPage('touch:bvl-franchise', routes.franchise)
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице мои отзывы', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('schema', {
                                    users: schema.users,
                                    modelOpinions: schema.modelOpinions,
                                    gradesOpinions: schema.gradesOpinions,
                                });
                                await this.browser.setState('report', report());
                                return this.browser.yaOpenPage('touch:user-reviews', {publicId})
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                }),
                makeSuite('На странице избранное', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'm-touch-2405',
                            issue: 'MOBMARKET-9940',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('persBasket', {
                                    items: [wishlishItem],
                                });
                                await this.browser.setState('report', report());

                                return this.browser.yaOpenPage('touch:wishlist')
                                    .yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                    }),
                })
                // Сравнение
            ),
        })
    ),
});
