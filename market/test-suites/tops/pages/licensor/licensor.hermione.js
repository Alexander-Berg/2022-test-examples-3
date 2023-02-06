import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/licensor';
// suites
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import BaseOpenGraphSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph';
// page-objects
import Base from '@self/platform/spec/page-objects/n-base';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница лицензиара.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('SEO-разметка страницы.', {
            environment: 'testing',
            story: mergeSuites(
                {
                    beforeEach() {
                        const queryParams = routes.licensor;
                        return this.browser.yaOpenPage('touch:bvl-licensor', queryParams);
                    },
                },

                makeSuite('SEO-разметка страницы.', {
                    story: createStories(
                        seoTestConfigs,
                        ({testParams}) => prepareSuite(BaseLinkCanonicalSuite, {
                            hooks: {
                                beforeEach() {
                                    return this.browser
                                        .yaSimulateBot();
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
                    params: {
                        expectedCanonicalLink: 'https://market.yandex.ru/licensor--cartoon-network/15948003',
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
                    params: {
                        expectedOpenGraphDescription: 'Каталог товаров с персонажами Cartoon Network ' +
                            'на Яндекс.Маркете. Цены, характеристики, широкий ассортимент товаров.',
                        expectedTitle: 'Cartoon Network — Каталог товаров с персонажами' +
                        ' Cartoon Network — Яндекс.Маркет',
                        expectedCanonicalLink: 'https://market.yandex.ru/licensor--cartoon-network/15948003',
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
