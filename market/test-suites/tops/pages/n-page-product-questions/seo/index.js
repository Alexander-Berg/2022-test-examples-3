import {merge, assign} from 'lodash';
import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// suites
import AlternateUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/alternate-url';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
// page-objects
import AlternateUrl from '@self/platform/spec/page-objects/alternate-url';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';

import productSeoMock, {productId, slug} from './mocks/product.mock';
import userMock from './mocks/user.mock.json';
import questionMock from './mocks/question.mock';

const PRODUCT_QUESTIONS_ROUT_NAME = 'market:product-questions';
const MOBILE_MARKET_HOST_NAME = 'm.market.yandex.ru';

export default makeSuite('SEO-разметка страницы.', {
    story: merge(
        createStories(
            productSeoMock,
            productMock => mergeSuites(
                prepareSuite(AlternateUrlSuite, {
                    meta: {
                        id: 'marketfront-2331',
                        issue: 'MARKETVERSTKA-31330',
                    },
                    pageObjects: {
                        alternateUrl() {
                            return this.createPageObject(AlternateUrl);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const routeParams = {
                                productId,
                                slug,
                            };

                            await this.browser.setState('schema', {
                                users: [userMock],
                                modelQuestions: [questionMock],
                            });

                            const expectedUrlPath = await this.browser.yaBuildURL(
                                PRODUCT_QUESTIONS_ROUT_NAME,
                                routeParams
                            );

                            await this.browser.setState('report', productMock);

                            assign(this.params, {
                                expectedUrl: `https://${MOBILE_MARKET_HOST_NAME}${expectedUrlPath}`,
                            });

                            return this.browser.yaOpenPage(PRODUCT_QUESTIONS_ROUT_NAME, routeParams);
                        },
                    },
                }),
                prepareSuite(CanonicalUrlSuite, {
                    meta: {
                        id: 'marketfront-2404',
                        issue: 'MARKETVERSTKA-31330',
                    },
                    pageObjects: {
                        canonicalUrl() {
                            return this.createPageObject(CanonicalUrl);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const routeParams = {
                                productId,
                                slug,
                            };

                            assign(this.params, {
                                expectedUrl: await this.browser.yaBuildURL(PRODUCT_QUESTIONS_ROUT_NAME, routeParams),
                            });

                            await this.browser.setState('schema', {
                                users: [userMock],
                                modelQuestions: [questionMock],
                            });

                            await this.browser.setState('report', productMock);
                            await this.browser.yaOpenPage(PRODUCT_QUESTIONS_ROUT_NAME, routeParams);
                        },
                    },
                })
            )
        )
    ),
});
