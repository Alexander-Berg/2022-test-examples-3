import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

// suites
import SlugTagsSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/tags';
import SlugCheckLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/checkLink';
// page-objects
import Base from '@self/platform/spec/page-objects/n-base';
import FooterMarket from '@self/platform/spec/page-objects/footer-market';
import Breadcrumbs from '@self/platform/spec/page-objects/n-breadcrumbs';
import CatalogHeader from '@self/platform/spec/page-objects/widgets/content/CatalogHeader';
// mocks
import {report, productId, shop, shopId, shopSlug} from './mock';

export default makeSuite('Общие тесты', {
    story: mergeSuites(
        prepareSuite(SlugTagsSuite, {
            params: {
                entity: 'product',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', report);
                    await this.browser.yaOpenPage('market:product', {
                        productId,
                        slug: 'product',
                    });
                },
            },
            pageObjects: {
                base() {
                    return this.createPageObject(Base);
                },
                footer() {
                    return this.createPageObject(FooterMarket);
                },
            },
        }),
        makeSuite('Хлебные крошки', {
            story: {
                'Страница КМ.': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'marketfront-3022',
                        issue: 'MARKETVERSTKA-33220',
                    },
                    params: {
                        entity: 'catalog',
                        selector: `${Breadcrumbs.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);

                            return this.browser.yaOpenPage('market:product', {
                                productId,
                                slug: 'kofemolka-bosch-mkm-6000-6003',
                            });
                        },
                    },
                }),
                'Страница департамента.': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        environment: 'testing',
                        id: 'marketfront-3022',
                        issue: 'MARKETVERSTKA-33220',
                    },
                    params: {
                        entity: 'catalog',
                        selector: `${CatalogHeader.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            return this.browser.yaOpenPage('market:catalog', {
                                nid: 54437,
                                slug: 'departament',
                            });
                        },
                    },
                }),
                'Страница выдачи.': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'marketfront-3022',
                        issue: 'MARKETVERSTKA-33220',
                    },
                    params: {
                        entity: 'catalog',
                        selector: `${Breadcrumbs.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('market:list', {
                                nid: 54726,
                                slug: 'mobilnye-telefony',
                            });
                        },
                    },
                }),
                'Страница отзывов о магазине.': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'marketfront-3022',
                        issue: 'MARKETVERSTKA-33220',
                    },
                    params: {
                        entity: 'shop',
                        selector: `${Breadcrumbs.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', shop);
                            return this.browser.yaOpenPage('market:shop-reviews', {
                                shopId,
                                slug: shopSlug,
                            });
                        },
                    },
                }),
            },
        })
    ),
});
