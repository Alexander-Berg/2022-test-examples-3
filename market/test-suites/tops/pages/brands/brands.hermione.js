import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/brand-page';
// suites
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import BaseOpenGraphSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph';
// page-objects
import Base from '@self/platform/spec/page-objects/n-base';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница бренда.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('SEO-разметка страницы.', {
            environment: 'testing',
            story: mergeSuites(
                makeSuite('SEO-разметка страницы.', {
                    story: createStories(
                        seoTestConfigs,
                        ({testParams}) => prepareSuite(BaseLinkCanonicalSuite, {
                            hooks: {
                                async beforeEach() {
                                    await this.browser.yaSimulateBot();
                                    await this.browser.yaOpenPage('touch:brands', routes.brand);
                                },
                            },
                            pageObjects: {
                                base() {
                                    return this.createPageObject(Base);
                                },
                            },
                            params: testParams,
                        })
                    ),
                }),

                prepareSuite(BaseLinkCanonicalSuite, {
                    meta: {
                        id: 'm-touch-1040',
                        issue: 'MOBMARKET-6323',
                    },
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage('touch:brands', routes.brand);
                        },
                    },
                    params: {
                        expectedCanonicalLink: 'https://market.yandex.ru/brands--samsung/153061',
                    },
                    pageObjects: {
                        base() {
                            return this.createPageObject(Base);
                        },
                    },
                }),

                prepareSuite(BaseOpenGraphSuite, {
                    meta: {
                        id: 'm-touch-1041',
                        issue: 'MOBMARKET-6320',
                    },
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage('touch:brands', routes.brand);
                        },
                    },
                    params: {
                        expectedOpenGraphDescription: 'Популярные товары бренда Samsung на Маркете. ' +
                        'Каталог товаров производителя: цены, характеристики, обзоры, ' +
                        'обсуждения, отзывы и оценки покупателей.',
                        expectedTitle: 'Samsung — Каталог товаров — Яндекс.Маркет',
                        expectedCanonicalLink: 'https://market.yandex.ru/brands--samsung/153061',
                    },
                    pageObjects: {
                        base() {
                            return this.createPageObject(Base);
                        },
                    },
                })
            ),
        })
    ),
});
