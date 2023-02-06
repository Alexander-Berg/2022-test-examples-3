import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';
// Suites
import SearchPagerSuite from '@self/platform/spec/hermione2/test-suites/blocks/SearchPager';
import SearchResultsSuite from '@self/platform/spec/hermione2/test-suites/blocks/SearchResults';
import SearchIntentsSuite from '@self/platform/spec/hermione2/test-suites/blocks/SearchIntents';
import BreadcrumbsSuite from '@self/platform/spec/hermione2/test-suites/blocks/Breadcrumbs';
import NothingFoundSuite from '@self/platform/spec/hermione2/test-suites/blocks/NothingFound';
// PageObjects
import SearchPager from '@self/platform/widgets/content/search/Pager/__pageObject';
import SnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
import ViewSwitcher from '@self/platform/widgets/content/search/ViewSwitcher/__pageObject';
import {Spin} from '@self/root/src/uikit/components/Spin/__pageObject';
import Search2 from '@self/platform/spec/page-objects/search2';
import NothingFoundSins from '@self/root/src/components/NothingFoundSins/__pageObject';
import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';
import IntentsTree from '@self/root/src/widgets/content/search/Intents/components/IntentsTree/__pageObject';
// KadavriqueState
import {createState} from './fixtures/reportSearch';

const businessId = '10671581';
const text = 'линзы';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('SinS', {
    story: mergeSuites(
        makeSuite('Поисковая страница', {
            story: mergeSuites(
                prepareSuite(SearchPagerSuite, {
                    meta: {
                        environment: 'kadavr',
                    },
                    hooks: {
                        async beforeEach() {
                            hermione.setPageObjects.call(this, {
                                searchPager: () => this.browser.createPageObject(SearchPager),
                            });

                            await this.browser.setState('report', createState());

                            return this.browser.yaOpenPage('market:list', {
                                businessId,
                                text,
                                nid: 54734,
                                hid: 8475840,
                                slug: 'tovary-dlia-zdorovia',
                            });
                        },
                    },
                    params: {
                        query: {businessId},
                    },
                }),
                prepareSuite(SearchResultsSuite, {
                    meta: {
                        environment: 'kadavr',
                    },
                    hooks: {
                        async beforeEach() {
                            hermione.setPageObjects.call(this, {
                                snippetCard: () => this.browser.createPageObject(SnippetCard),
                                snippetCell: () => this.browser.createPageObject(SnippetCell),
                                viewSwitcher: () => this.browser.createPageObject(ViewSwitcher),
                                spin: () => this.browser.createPageObject(Spin),
                            });

                            await this.browser.setState('report', createState());

                            return this.browser.yaOpenPage('market:search', {businessId, text});
                        },
                    },
                    params: {query: {businessId}},
                }),
                prepareSuite(NothingFoundSuite, {
                    meta: {
                        environment: 'kadavr',
                    },
                    hooks: {
                        async beforeEach() {
                            hermione.setPageObjects.call(this, {
                                search2: () => this.browser.createPageObject(Search2),
                                nothingFoundSins: () => this.browser.createPageObject(NothingFoundSins),
                            });

                            await this.browser.setState('report', createState(0));

                            return this.browser.yaOpenPage('market:business', {
                                slug: 'slug',
                                businessId,
                            });
                        },
                    },
                    params: {
                        query: {businessId},
                    },
                }),
                prepareSuite(BreadcrumbsSuite, {
                    meta: {
                        environment: 'kadavr',
                    },
                    hooks: {
                        async beforeEach() {
                            hermione.setPageObjects.call(this, {
                                breadcrumbs: () => this.browser.createPageObject(Breadcrumbs),
                            });

                            await this.browser.setState('report', createState());

                            return this.browser.yaOpenPage('market:list', {
                                businessId,
                                text,
                                nid: 54726,
                                hid: 91491,
                                slug: 'mobilnye-telefony',
                            });
                        },
                    },
                    params: {
                        query: {businessId},
                    },
                }),
                prepareSuite(SearchIntentsSuite, {
                    meta: {
                        environment: 'kadavr',
                    },
                    hooks: {
                        async beforeEach() {
                            hermione.setPageObjects.call(this, {
                                intentsTree: () => this.browser.createPageObject(IntentsTree),
                            });

                            await this.browser.setState('report', createState());

                            return this.browser.yaOpenPage('market:search', {businessId, text});
                        },
                    },
                    params: {
                        query: {businessId},
                    },
                })
            ),
        })
    ),
});
