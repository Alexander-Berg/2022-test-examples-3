import {merge} from 'lodash';
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-reviews-page';
import PageH1Suite from '@self/platform/spec/hermione/test-suites/blocks/page-h1';
import BaseOpenGraphSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph';
import ReviewFormProductTitle from '@self/platform/spec/page-objects/n-review-form-product-title';
import Base from '@self/platform/spec/page-objects/n-base';

const PRODUCT_REVIEWS_ADD_ROUT_NAME = 'market:product-reviews-add';

export default makeSuite('Страница добавления отзыва.', {
    environment: 'testing',
    issue: 'MARKETVERSTKA-29820',
    story: merge(
        {
            'SEO-разметка страницы.': mergeSuites(
                prepareSuite(PageH1Suite, {
                    meta: {
                        environment: 'testing',
                        issue: 'MARKETVERSTKA-29820',
                        id: 'marketfront-2405',
                    },
                    pageObjects: {
                        headline() {
                            return this.createPageObject(ReviewFormProductTitle);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const routeParams = seoTestConfigs.canonicalDirectOpening.routeParams;

                            await this.browser.yaOpenPage(PRODUCT_REVIEWS_ADD_ROUT_NAME, routeParams);
                        },
                    },
                    params: seoTestConfigs.canonicalDirectOpening.testParams,
                })
            ),
        },
        createStories(
            seoTestConfigs.pageOpenGraph,
            ({routeParams, testParams}) => prepareSuite(BaseOpenGraphSuite, {
                meta: {
                    id: 'marketfront-2426',
                    issue: 'MARKETVERSTKA-29820',
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage(PRODUCT_REVIEWS_ADD_ROUT_NAME, routeParams);
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
                params: testParams,
            })
        )
    ),
});
