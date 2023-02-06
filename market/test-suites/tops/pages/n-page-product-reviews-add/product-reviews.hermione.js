import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-reviews-page';
// suites
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
// page-objects
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
// parts
import Seo from './seo';
import ProductReviewsPlusPayment from './productReviewsPlusPayment';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница добавления отзыва.', {
    environment: 'testing',
    issue: 'MARKETVERSTKA-24456',
    story: mergeSuites(
        makeSuite('SEO-разметка страницы.', {
            story: mergeSuites(
                prepareSuite(CanonicalUrlSuite, {
                    pageObjects: {
                        canonicalUrl() {
                            return this.createPageObject(CanonicalUrl);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            const {routeParams} = seoTestConfigs.canonicalUrl;

                            return this.browser.yaOpenPage('market:product-reviews-add', routeParams);
                        },
                    },
                    params: seoTestConfigs.canonicalUrl.testParams,
                }),
                prepareSuite(PageTitleSuite, {
                    hooks: {
                        beforeEach() {
                            const {routeParams} = seoTestConfigs.pageTitle;

                            return this.browser.yaOpenPage('market:product-reviews-add', routeParams);
                        },
                    },
                    params: seoTestConfigs.pageTitle.testParams,
                }),
                prepareSuite(PageDescriptionSuite, {
                    hooks: {
                        beforeEach() {
                            const {routeParams} = seoTestConfigs.pageDescription;

                            return this.browser.yaOpenPage('market:product-reviews-add', routeParams);
                        },
                    },
                    meta: {
                        environment: 'testing',
                    },
                    pageObjects: {
                        pageMeta() {
                            return this.createPageObject(PageMeta);
                        },
                    },
                    params: seoTestConfigs.pageDescription.testParams,
                })
            ),
        }),
        Seo,
        ProductReviewsPlusPayment
    ),
});
