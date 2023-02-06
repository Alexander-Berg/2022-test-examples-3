import {merge} from 'lodash';
import {prepareSuite, makeSuite} from 'ginny';

// helpers
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

// import productSeoMock, {productId, slug} from './mocks/product.mock';

const PRODUCT_SPEC_ROUT_NAME = 'market:product-spec';

export default makeSuite('SEO-разметка страницы.', {
    story: merge(
        createStories(
            seoTestConfigs.pageMainHeader,
            ({routeParams, testParams, mock}) => prepareSuite(PageH1Suite, {
                meta: {
                    id: 'marketfront-2333',
                    issue: 'MARKETVERSTKA-27964',
                },
                pageObjects: {
                    headline() {
                        return this.createPageObject(ProductTitle);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', mock);
                        return this.browser.yaOpenPage(PRODUCT_SPEC_ROUT_NAME, routeParams);
                    },
                },
                params: testParams.specs,
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
                            return this.browser.yaOpenPage(PRODUCT_SPEC_ROUT_NAME, routeParams);
                        },
                    },
                    params: testParams.specs,
                }),
            })
        ),
        createStories(
            seoTestConfigs.pageDescription,
            ({testParams, routeParams, mock}) => prepareSuite(PageDescriptionSuite, {
                hooks: {
                    async beforeEach() {
                        await applyCompoundState(this.browser, createProductWithSchemaAndOffer(mock));
                        return this.browser.yaOpenPage(PRODUCT_SPEC_ROUT_NAME, routeParams);
                    },
                },
                pageObjects: {
                    pageMeta() {
                        return this.createPageObject(PageMeta);
                    },
                },
                params: testParams.spec,
            })
        )
    ),
});
