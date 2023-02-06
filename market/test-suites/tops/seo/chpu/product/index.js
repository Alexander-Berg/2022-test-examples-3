import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {routes} from '@self/platform/spec/hermione/configs/routes';

// suites
import SlugCheckLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/checkLink';
// mocks
import {report, schema, userPublicId as publicId, wishlishItem} from './mocks';
import articlePageMock from './article.mock';
import collectionsPageMock from './collections.mock';
import brandPageMock from './brand.mock';
import franchisePageMock from './franchise.mock';

const suiteParams = {
    entity: 'product',
};

export default makeSuite('Продукт', {
    story: mergeSuites(
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
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:index');
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
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:catalog', routes.catalog.electronics);
                            },
                        },
                    }),
                }),
                makeSuite('На странице выдачи', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:list', routes.list.phones);
                            },
                        },
                    }),
                }),
                makeSuite('На странице выдачи + отзывы покупателей', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:list', routes.list.reviews);
                            },
                        },
                    }),
                }),
                makeSuite('На странице статьи', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [articlePageMock]);
                                await this.browser.yaOpenPage('market:articles', routes.articles.kofemolki);
                            },
                        },
                    }),
                }),
                makeSuite('На странице подборки', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [collectionsPageMock.Tarantino]);
                                await this.browser.setState('report', collectionsPageMock.report);
                                await this.browser.yaOpenPage('market:collections', routes.collections);
                            },
                        },
                    }),
                }),
                makeSuite('На странице статьи Журнала', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage(
                                    'market:journal-article',
                                    routes.journalArticle.holodilnik
                                );
                            },
                        },
                    }),
                }),
                makeSuite('На странице КМ', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            environment: 'testing',
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:product', routes.product.phone);
                            },
                        },
                    }),
                }),
                makeSuite('На странице бренда', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [brandPageMock.Tarantino]);
                                await this.browser.setState('report', brandPageMock.report);
                                await this.browser.yaOpenPage('market:brands', routes.brand);
                            },
                        },
                    }),
                }),
                makeSuite('На странице франшизы', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('Tarantino.data.result', [franchisePageMock.Tarantino]);
                                await this.browser.setState('report', franchisePageMock.report);
                                await this.browser.yaOpenPage('market:bvl-franchise', routes.franchise.starwars);
                            },
                        },
                    }),
                }),
                makeSuite('На странице мои отзывы', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('schema', schema);
                                await this.browser.setState('report', report());

                                await this.browser.yaOpenPage('market:user-reviews', {publicId});
                            },
                        },
                    }),
                }),
                makeSuite('На странице избранное', {
                    story: prepareSuite(SlugCheckLinkSuite, {
                        meta: {
                            id: 'marketfront-3092',
                            issue: 'MARKETVERSTKA-30914',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaProfile('ugctest3', 'market:wishlist');
                                await this.browser.setState('persBasket', {
                                    items: [wishlishItem],
                                });
                                await this.browser.setState('report', report());

                                return this.browser.yaOpenPage('market:wishlist');
                            },
                            async afterEach() {
                                return this.browser.yaLogout();
                            },
                        },
                    }),
                })
            ),
        })
    ),
});
