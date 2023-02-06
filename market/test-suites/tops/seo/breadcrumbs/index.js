import {merge} from 'lodash';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {applyCompoundState} from '@self/project/src/spec/hermione/helpers/metakadavr';
// suites
import BreadcrumbsNoDuplicateSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/no-duplicate';
import BreadcrumbsEqualTextWithHeaderSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/equal-text-with-header';
import BreadcrumbsResetBonClickableSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/reset-non-clickable';
import BreadcrumbsHasAllBreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/has-all-breadcrumbs';
import BreadcrumbsVendorSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/vendor';
import BreadcrumbsLastNoClickableSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/last-no-clickable';
// page-objects
import Headline from '@self/root/src/widgets/content/search/Title/components/Title/__pageObject';
import PlatformBreadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';
import Breadcrumbs from '@self/root/src/widgets/content/search/Breadcrumbs/components/Breadcrumbs/__pageObject';
import FilterList from '@self/platform/spec/page-objects/FilterList';
import ShopReviews from '@self/platform/spec/page-objects/widgets/content/ShopReviews';

import catalogConfig from './configs/catalog';
import productConfig from './configs/product';
import shopConfig from './configs/shop';

export default mergeSuites(
    {
        beforeEach() {
            this.setPageObjects({
                breadcrumbs: () => this.createPageObject(Breadcrumbs),
            });
        },
    },
    makeSuite('Страница выдачи', {
        story: mergeSuites(
            createStories(
                [
                    catalogConfig.guru,
                    catalogConfig.guruLight,
                    catalogConfig.clustered,
                ],
                ({routeParams, mocks}) => prepareSuite(BreadcrumbsNoDuplicateSuite, {
                    hooks: {
                        async beforeEach() {
                            this.setPageObjects({
                                breadcrumbs: () => this.createPageObject(Breadcrumbs),
                            });

                            await applyCompoundState(this.browser, mocks);
                            return this.browser.yaOpenPage('market:list', routeParams);
                        },
                    },
                })
            ),
            createStories(
                [
                    catalogConfig.guruWithRecipe,
                    catalogConfig.guruWithParametrizedHeader,
                    catalogConfig.guruLightWithRecipe,
                    catalogConfig.clusteredWithRecipe,
                ],
                ({routeParams, mocks, params: {breadcrumbsText}}) => mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                breadcrumbs: () => this.createPageObject(Breadcrumbs),
                            });

                            await applyCompoundState(this.browser, mocks);
                            return this.browser.yaOpenPage('market:list', routeParams);
                        },
                    },
                    prepareSuite(BreadcrumbsNoDuplicateSuite),
                    prepareSuite(BreadcrumbsEqualTextWithHeaderSuite, {
                        pageObjects: {
                            headline() {
                                return this.createPageObject(Headline);
                            },
                        },
                    }),
                    prepareSuite(BreadcrumbsResetBonClickableSuite, {
                        pageObjects: {
                            headline() {
                                return this.createPageObject(Headline);
                            },
                            filter() {
                                return this.createPageObject(FilterList);
                            },
                        },
                    }),
                    prepareSuite(BreadcrumbsHasAllBreadcrumbsSuite, {
                        params: {breadcrumbsText},
                    })
                )
            )
        ),
    }),
    makeSuite('Страница продукта', {
        story: mergeSuites(
            merge(
                {
                    beforeEach() {
                        this.setPageObjects({
                            breadcrumbs: () => this.createPageObject(PlatformBreadcrumbs),
                        });
                    },
                },
                createStories(
                    productConfig,
                    ({routeParams, mocks}) => prepareSuite(BreadcrumbsNoDuplicateSuite, {
                        hooks: {
                            async beforeEach() {
                                await applyCompoundState(this.browser, mocks);
                                return this.browser.yaOpenPage('market:product', routeParams);
                            },
                        },
                    })
                ),
                createStories(
                    [productConfig.guru],
                    ({routeParams, mocks}) => prepareSuite(BreadcrumbsVendorSuite, {
                        hooks: {
                            async beforeEach() {
                                await applyCompoundState(this.browser, mocks);
                                return this.browser.yaOpenPage('market:product', routeParams);
                            },
                        },
                    })
                )
            )
        ),
    }),

    makeSuite('Страница отзыва о магазине', {
        story: createStories(
            shopConfig,
            ({routeParams, mocks, params: {breadcrumbsText}}) => mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            breadcrumbs: () => this.createPageObject(PlatformBreadcrumbs),
                        });

                        if (mocks) {
                            await applyCompoundState(this.browser, mocks);
                        }
                        return this.browser.yaOpenPage('market:shop-reviews', routeParams);
                    },
                },
                prepareSuite(BreadcrumbsNoDuplicateSuite),
                prepareSuite(BreadcrumbsLastNoClickableSuite),
                prepareSuite(BreadcrumbsEqualTextWithHeaderSuite, {
                    pageObjects: {
                        headline() {
                            return this.createPageObject(ShopReviews);
                        },
                    },
                }),
                prepareSuite(BreadcrumbsHasAllBreadcrumbsSuite, {
                    params: {breadcrumbsText},
                })
            )
        ),
    })
);
