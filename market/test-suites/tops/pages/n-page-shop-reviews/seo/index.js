import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers/shop';

// configs
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/shop-reviews-page';
// suites
import PageH1Suite from '@self/platform/spec/hermione/test-suites/blocks/page-h1';
import BreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs';
import BreadcrumbsItemClickableYesSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/__item_clickable_yes';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
import SchemaOrgOrganizationSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/organization';
import SchemaOrgReviewSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/review';

// page-objects
import ShopHub from '@self/platform/spec/page-objects/n-shop-hub';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
import SchemaOrgOrganization from '@self/platform/spec/page-objects/SchemaOrgOrganization';
import SchemaOrgAggregateRating from '@self/platform/spec/page-objects/SchemaOrgAggregateRating';
import SchemaOrgPostalAddress from '@self/platform/spec/page-objects/SchemaOrgPostalAddress';
import ShopReviewsContainer from '@self/platform/spec/page-objects/n-shop-reviews-container';
import SchemaOrgRating from '@self/platform/spec/page-objects/SchemaOrgRating';
import SchemaOrgReview from '@self/platform/spec/page-objects/SchemaOrgReview';
import ProductReviewItem from '@self/platform/spec/page-objects/n-product-review-item';
import ShopReviewsList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';
import SchemaOrgBreadcrumbsList from '@self/platform/spec/page-objects/SchemaOrgBreadcrumbsList';
import SchemaOrgBreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/breadcrumbs';

import ShopReviews from '@self/platform/spec/page-objects/widgets/content/ShopReviews';
import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';

// mocks
import shopInfoMockData from './mocks/shopInfo.mock.json';
import shopDistributionMockData from './mocks/shopDistribution.mock.json';
import userMock from './mocks/user.mock.json';
import shopReviewMock from './mocks/shopReview.mock';

export default makeSuite('SEO-разметка страницы.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(PageH1Suite, {
            meta: {
                environment: 'testing',
                id: 'marketfront-2333',
                issue: 'MARKETVERSTKA-27964',
            },
            pageObjects: {
                headline() {
                    return this.createPageObject(ShopReviews);
                },
            },
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage(
                        'market:shop-reviews',
                        seoTestConfigs.pageMainHeader.routeParams
                    );
                },
            },
            params: seoTestConfigs.pageMainHeader.testParams,
        }),
        mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        breadcrumbs: () => this.createPageObject(Breadcrumbs),
                        headline: () => this.createPageObject(ShopHub),
                    });

                    const {routeParams} = seoTestConfigs.breadcrumbs;
                    return this.browser.yaOpenPage('market:shop-reviews', routeParams);
                },
            },
            prepareSuite(BreadcrumbsSuite, {
                params: {
                    urlMatcher: 'shop--.*/\\d+',
                },
            }),
            prepareSuite(BreadcrumbsItemClickableYesSuite)
        ),
        prepareSuite(PageDescriptionSuite, {
            hooks: {
                async beforeEach() {
                    const {
                        routeParams,
                        newGradesCount,
                        ratingToShow,
                        shopName,
                    } = seoTestConfigs.pageDescription;
                    await this.browser.setState('report', createShopInfo({
                        newGradesCount,
                        ratingToShow,
                        shopName,
                    }, routeParams.shopId));

                    return this.browser.yaOpenPage('market:shop-reviews', routeParams);
                },
            },
            pageObjects: {
                pageMeta() {
                    return this.createPageObject(PageMeta);
                },
            },
            params: seoTestConfigs.pageDescription.testParams,
        }),
        prepareSuite(SchemaOrgOrganizationSuite, {
            hooks: {
                async beforeEach() {
                    const {testParams} = seoTestConfigs.schemaOrgOrganization;

                    Object.assign(this.params, {
                        expectedName: shopInfoMockData.shopName}, testParams);

                    const schemaOrgOrganization = this.createPageObject(
                        SchemaOrgOrganization,
                        {
                            parent: this.createPageObject(ShopReviewsContainer),
                        }
                    );

                    const schemaOrgPostalAddress = this.createPageObject(
                        SchemaOrgPostalAddress,
                        {
                            parent: schemaOrgOrganization,
                        }
                    );

                    const schemaOrgAggregateRating = this.createPageObject(
                        SchemaOrgAggregateRating,
                        {
                            parent: schemaOrgOrganization,
                        }
                    );

                    this.setPageObjects({
                        schemaOrgOrganization: () => schemaOrgOrganization,
                        schemaOrgPostalAddress: () => schemaOrgPostalAddress,
                        schemaOrgAggregateRating: () => schemaOrgAggregateRating,
                    });

                    await this.browser
                        .setState(
                            'report',
                            createShopInfo(shopInfoMockData, shopInfoMockData.id)
                        ).setState('schema', {
                            users: [userMock],
                            modelOpinions: [shopReviewMock],
                            shopGrades: shopDistributionMockData,
                        });

                    return this.browser.yaOpenPage('market:shop-reviews', {
                        shopId: shopInfoMockData.id,
                        slug: shopInfoMockData.slug,
                    });
                },
            },
        }),
        prepareSuite(SchemaOrgReviewSuite, {
            hooks: {
                async beforeEach() {
                    const schemaOrgReview = this.createPageObject(SchemaOrgReview, {
                        parent: this.createPageObject(ProductReviewItem, {
                            parent: this.createPageObject(ShopReviewsList),
                        }),
                    });

                    this.setPageObjects({
                        schemaOrgReview: () => schemaOrgReview,
                        schemaOrgRating: () => this.createPageObject(SchemaOrgRating, {
                            parent: schemaOrgReview,
                        }),
                    });

                    await this.browser
                        .setState('report', createShopInfo(shopInfoMockData, shopInfoMockData.id))
                        .setState('schema', {
                            users: [userMock],
                            modelOpinions: [shopReviewMock],
                        });

                    return this.browser.yaOpenPage('market:shop-reviews', {
                        shopId: shopInfoMockData.id,
                        slug: shopInfoMockData.slug,
                    });
                },
            },
        }),
        prepareSuite(SchemaOrgBreadcrumbsSuite, {
            hooks: {
                async beforeEach() {
                    const schemaOrgOrganization = this.createPageObject(
                        SchemaOrgOrganization,
                        {
                            parent: this.createPageObject(ShopReviewsContainer),
                        }
                    );

                    const schemaOrgBreadcrumbsList = this.createPageObject(
                        SchemaOrgBreadcrumbsList,
                        {
                            parent: schemaOrgOrganization,
                        }
                    );

                    this.setPageObjects({
                        schemaOrgBreadcrumbsList: () => schemaOrgBreadcrumbsList,
                    });

                    await this.browser.setState('report', createShopInfo(shopInfoMockData, shopInfoMockData.id));

                    return this.browser.yaOpenPage('market:shop-reviews', {
                        shopId: shopInfoMockData.id,
                        slug: shopInfoMockData.slug,
                    });
                },
            },
        })
    ),
});

