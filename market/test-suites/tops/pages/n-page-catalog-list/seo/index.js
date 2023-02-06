import {merge, assign} from 'lodash';
import {prepareSuite, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/catalog-page';
import {applyCompoundState} from '@self/project/src/spec/hermione/helpers/metakadavr';
// suites
import PageH1Suite from '@self/platform/spec/hermione/test-suites/blocks/page-h1';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
import MetaRobotsSuite from '@self/project/src/spec/hermione/test-suites/blocks/MetaRobots';
// page-objects
import Headline from '@self/root/src/widgets/content/search/Title/components/Title/__pageObject';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';

export default makeSuite('SEO-разметка страницы.', {
    story: merge(
        createStories(
            seoTestConfigs.canonicalDirectOpening,
            ({routeParams, testParams, compoundMock}) => makeSuite('Переход по canonical-url.', {
                story: prepareSuite(PageH1Suite, {
                    meta: {
                        issue: 'MARKETVERSTKA-28692',
                        id: 'marketfront-2405',
                        environment: 'kadavr',
                    },
                    pageObjects: {
                        headline() {
                            return this.createPageObject(Headline);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await applyCompoundState(this.browser, compoundMock);

                            return this.browser.yaOpenPage('market:list', routeParams);
                        },
                    },
                    params: testParams,
                }),
            })
        ),
        createStories(
            seoTestConfigs.canonicalUrl,
            ({testParams, routeParams}) => prepareSuite(CanonicalUrlSuite, {
                pageObjects: {
                    canonicalUrl() {
                        return this.createPageObject(CanonicalUrl);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage(
                            'market:catalog', assign({_mod: 'robot'}, routeParams)
                        );
                    },
                },
                params: testParams,
            })
        ),
        createStories(
            seoTestConfigs.pageDescription,
            ({testParams, routeParams, compoundMock}) => prepareSuite(PageDescriptionSuite, {
                hooks: {
                    async beforeEach() {
                        await applyCompoundState(this.browser, compoundMock);
                        return this.browser.yaOpenPage('market:list', routeParams);
                    },
                },
                pageObjects: {
                    pageMeta() {
                        return this.createPageObject(PageMeta);
                    },
                },
                params: testParams,
            })
        ),
        createStories(
            seoTestConfigs.robots,
            ({testParams, routeParams, compoundMock, meta}) => prepareSuite(MetaRobotsSuite, {
                meta,
                pageObjects: {
                    pageMeta() {
                        return this.createPageObject(PageMeta);
                    },
                },
                hooks: {
                    async beforeEach() {
                        if (compoundMock) {
                            await applyCompoundState(this.browser, compoundMock);
                        } else {
                            await this.browser.setState(
                                'report',
                                {data: {
                                    search: {
                                        total: 0,
                                        totalOffers: 0,
                                        totalOffersBeforeFilters: 0,
                                        filters: ['glprice', 'included-in-price'],
                                        results: [],
                                    },
                                }}
                            );
                        }

                        return this.browser.yaOpenPage(
                            'market:list', assign({_mod: 'robot'}, routeParams)
                        );
                    },
                },
                params: testParams,
            })
        )
    ),
});
