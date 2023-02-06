import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

// suites
import SlugTagsSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/tags';
import SlugCheckLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/slug/checkLink';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Base from '@self/platform/spec/page-objects/n-base';
import Footer from '@self/platform/spec/page-objects/Footer';
import ProductCardBreadcrumbs from '@self/platform/spec/page-objects/ProductCardBreadcrumbs';
import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';
import Card from '@self/project/src/components/Card/__pageObject';

import {
    report,
    productId,
    schema,
    shop,
    shopId,
    shopSlug,
    offer,
    offerId,
    cataloger,
} from './mock';

export default makeSuite('Общие тесты', {
    story: mergeSuites(
        prepareSuite(SlugTagsSuite, {
            params: {
                entity: 'product',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', report);
                    await this.browser.yaOpenPage('touch:product', {
                        productId,
                        slug: 'product',
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                base() {
                    return this.createPageObject(Base);
                },
                footer() {
                    return this.createPageObject(Footer);
                },
            },
        }),
        makeSuite('Хлебные крошки', {
            story: {
                'Страница КМ': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'catalog',
                        selector: `${BreadcrumbsUnified.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('touch:product', {
                                productId,
                                slug: 'product',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница характеристик КМ': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'product',
                        selector: `${BreadcrumbsUnified.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('touch:product-spec', {
                                productId,
                                slug: 'product',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница видеообзоров КМ': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'product',
                        selector: `${BreadcrumbsUnified.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('touch:product-videos', {
                                productId,
                                slug: 'product',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница отзывов о КМ': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'product',
                        selector: `${BreadcrumbsUnified.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('touch:product-reviews', {
                                productId,
                                slug: 'product',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница вопросов о КМ': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'product',
                        selector: `${BreadcrumbsUnified.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('touch:product-questions', {
                                productId,
                                slug: 'product',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница конкретного вопроса о КМ': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'product',
                        selector: '[data-autotest-id="title-card"] [data-autotest-id="questions"]',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', report);
                            return this.browser.yaOpenPage('touch:product-question', {
                                productId,
                                productSlug: 'product',
                                questionId: 1,
                                questionSlug: 'question',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница департамента': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        environment: 'testing',
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'catalog',
                        selector: `${BreadcrumbsUnified.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            return this.browser.yaOpenPage('touch:catalog', {
                                nid: 54437,
                                slug: 'departament',
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница отзывов о магазине': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'shop',
                        selector: `${Card.root} [data-autotest-id="backLink"]`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', shop);
                            return this.browser.yaOpenPage('touch:shop-reviews', {
                                shopId,
                                slug: shopSlug,
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница информации о доставки магазина': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'shop',
                        selector: `${ProductCardBreadcrumbs.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', shop);
                            return this.browser.yaOpenPage('touch:shop-delivery', {
                                shopId,
                                slug: shopSlug,
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница информации о магазине': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'shop',
                        selector: `${ProductCardBreadcrumbs.root} a`,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', shop);
                            return this.browser.yaOpenPage('touch:shop-info', {
                                shopId,
                                slug: shopSlug,
                            })
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
                'Страница оффера': prepareSuite(SlugCheckLinkSuite, {
                    meta: {
                        id: 'm-touch-2515',
                        issue: 'MOBMARKET-10847',
                    },
                    params: {
                        entity: 'catalog',
                        selector: '[data-autotest-id="breadcrumb"]',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('Cataloger.tree', cataloger);
                            await this.browser.setState('report', offer);
                            return this.browser.yaOpenPage('touch:offer', {offerId})
                                .yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                }),
            },
        })
    ),
});
