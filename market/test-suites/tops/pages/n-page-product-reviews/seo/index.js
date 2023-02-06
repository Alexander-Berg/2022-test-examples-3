import {merge} from 'lodash';
import {makeSuite, prepareSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {createProductWithSchemaAndOffer, applyCompoundState} from '@self/project/src/spec/hermione/helpers/metakadavr';
// configs
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';
// suites
import PageH1Suite from '@self/platform/spec/hermione/test-suites/blocks/page-h1';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
// import SchemaOrgBreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/schemaOrg/breadcrumbs';
// page-objects
import ProductTitle from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
// import SchemaOrgBreadcrumbsList from '@self/platform/spec/page-objects/SchemaOrgBreadcrumbsList';
// import ProductSummary from '@self/platform/spec/page-objects/n-product-summary';

// import {productSeoMock, productId, slug} from './mocks/product.mock';

const PRODUCT_REVIEWS_ROUT_NAME = 'market:product-reviews';

export default makeSuite('Страница отзывов на товар.', {
    environment: 'testing',
    issue: 'MARKETVERSTKA-24456',
    story: merge(
        createStories(
            seoTestConfigs.pageMainHeader,
            ({routeParams, testParams, mock}) => prepareSuite(PageH1Suite, {
                meta: {
                    issue: 'MARKETVERSTKA-27918',
                    id: 'marketfront-2330',
                },
                pageObjects: {
                    headline() {
                        return this.createPageObject(ProductTitle);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', mock);
                        return this.browser.yaOpenPage(PRODUCT_REVIEWS_ROUT_NAME, routeParams);
                    },
                },
                params: testParams.reviews,
            })
        ),
        createStories(
            seoTestConfigs.canonicalDirectOpening,
            ({routeParams, testParams, mock}) => makeSuite('Переход по canonical-url.', {
                story: prepareSuite(PageH1Suite, {
                    meta: {
                        issue: 'MARKETVERSTKA-28692',
                        id: 'marketfront-2405',
                    },
                    pageObjects: {
                        headline() {
                            return this.createPageObject(ProductTitle);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', mock);
                            return this.browser.yaOpenPage(PRODUCT_REVIEWS_ROUT_NAME, routeParams);
                        },
                    },
                    params: testParams.reviews,
                }),
            })
        ),
        createStories(
            seoTestConfigs.pageDescription,
            ({testParams, routeParams, mock}) => prepareSuite(PageDescriptionSuite, {
                hooks: {
                    async beforeEach() {
                        await applyCompoundState(this.browser, createProductWithSchemaAndOffer(mock));

                        return this.browser.yaOpenPage('market:product-reviews', routeParams);
                    },
                },
                pageObjects: {
                    pageMeta() {
                        return this.createPageObject(PageMeta);
                    },
                },
                params: testParams.reviews,
            })
        )
    ),
});
