import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createUser, createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {buildSlug} from '@self/platform/widgets/pages/VersusPage/meta';
// suites
import VersusContentProductCardSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/productCard';
import VersusContentHeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/header';
import VersusContentNoQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/noQuestions';
import VersusContentLessThanThreeQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/VersusContent/lessThanThreeQuestions';
import VersusContentMoreThanThreeQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/VersusContent/moreThanThreeQuestions';
import VersusContentNoReviewsSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/noReviews';
import VersusContentOneReviewWithTextSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/oneReviewWithText';
import VersusContentReviewsWithTextSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/reviewsWithText';
import VersusContentReviewsWithoutTextSuite from '@self/platform/spec/hermione/test-suites/blocks/VersusContent/reviewsWithoutText';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import PageTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/page-title';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
// page-objects
import ProductCard from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/ProductCard';
import Header from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/Header';
import Questions from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/Questions';
import Reviews from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/Reviews';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
// mocks
import {product as productMock} from './mocks/product.mock';
import {versus as versusMock} from './mocks/versus.mock';
import {question as questionMock} from './mocks/questions.mock';
import {review as reviewMock, grade as gradeMock} from './mocks/opinions.mock';

const versus = versusMock;
const product1 = createProduct(productMock, versus.products[0].id);
const product2 = createProduct(productMock, versus.products[1].id);
const user = createUser();
const question = createQuestion(questionMock);

const reviewMockWithUser = {
    ...reviewMock,
    user: {
        uid: user.uid.value,
        entity: 'user',
    },
};
const gradeMockWithUser = {
    ...gradeMock,
    user: {
        uid: user.uid.value,
        entity: 'user',
    },
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница автосравнений.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            'Основной контент страницы.': mergeSuites(
                prepareSuite(VersusContentProductCardSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                            };

                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);

                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        productCard() {
                            return this.createPageObject(ProductCard);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        slug: productMock.slug,
                    },
                }),
                prepareSuite(VersusContentHeaderSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);

                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        header() {
                            return this.createPageObject(Header);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        slug: productMock.slug,
                    },
                }),
                prepareSuite(VersusContentNoQuestionsSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                                modelQuestions: [],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);

                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        questions() {
                            return this.createPageObject(Questions);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        productSlug: productMock.slug,
                    },
                }),
                prepareSuite(VersusContentLessThanThreeQuestionsSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                                users: [user],
                                modelQuestions: [question],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);

                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        questions() {
                            return this.createPageObject(Questions);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        productSlug: productMock.slug,
                        questionId: questionMock.id,
                        questionSlug: questionMock.slug,
                    },
                }),
                prepareSuite(VersusContentMoreThanThreeQuestionsSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                                users: [user],
                                modelQuestions: [
                                    question,
                                    {...question, id: 111},
                                    {...question, id: 222},
                                    {...question, id: 333}],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        questions() {
                            return this.createPageObject(Questions);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        productSlug: productMock.slug,
                        questionId: questionMock.id,
                        questionSlug: questionMock.slug,
                    },
                }),
                prepareSuite(VersusContentNoReviewsSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                                users: [user],
                                gradesOpinions: [],
                                modelOpinions: [],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        reviews() {
                            return this.createPageObject(Reviews);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        slug: productMock.slug,
                    },
                }),
                prepareSuite(VersusContentOneReviewWithTextSuite, {
                    hooks: {
                        async beforeEach() {
                            const product = createProduct({...productMock, opinions: 1}, versus.products[0].id);

                            const schema = {
                                versus: [versusMock],
                                users: [user],
                                modelOpinions: [reviewMockWithUser],
                            };
                            const state = mergeState([product, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        reviews() {
                            return this.createPageObject(Reviews);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        slug: productMock.slug,
                    },
                }),
                prepareSuite(VersusContentReviewsWithTextSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                                users: [user],
                                modelOpinions: [
                                    reviewMockWithUser,
                                    {...reviewMockWithUser, id: '111'},
                                    {...reviewMockWithUser, id: '222'},
                                    {...reviewMockWithUser, id: '333'}],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        reviews() {
                            return this.createPageObject(Reviews);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        slug: productMock.slug,
                    },
                }),
                prepareSuite(VersusContentReviewsWithoutTextSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                versus: [versusMock],
                                users: [user],
                                gradesOpinions: [gradeMockWithUser, {...gradeMockWithUser, id: 11}],
                            };
                            const state = mergeState([product1, product2]);

                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:versus', {
                                id: versus.id,
                                slug: 'any',
                            });
                        },
                    },
                    pageObjects: {
                        reviews() {
                            return this.createPageObject(Reviews);
                        },
                    },
                    params: {
                        productId: productMock.id,
                        slug: productMock.slug,
                    },
                })
            ),
            'SEO-разметка страницы.': mergeSuites(
                {
                    async beforeEach() {
                        const schema = {
                            versus: [versusMock],
                        };

                        const state = mergeState([product1, product2]);

                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', state);

                        return this.browser.yaOpenPage('market:versus',
                            {
                                id: versus.id,
                                slug: buildSlug([productMock, productMock]),
                            });
                    },
                },

                prepareSuite(CanonicalUrlSuite, {
                    meta: {
                        environment: 'kadavr',
                        issue: 'MARKETFRONT-5153',
                        id: 'marketfront-3789',
                    },
                    pageObjects: {
                        canonicalUrl() {
                            return this.createPageObject(CanonicalUrl);
                        },
                    },
                    params: {
                        expectedUrl: `/versus--${productMock.slug}-vs-${productMock.slug}/${versus.id}`,
                    },
                }),

                prepareSuite(PageTitleSuite, {
                    meta: {
                        environment: 'kadavr',
                        issue: 'MARKETFRONT-5153',
                        id: 'marketfront-3790',
                    },
                    params: {
                        expectedTitle: `Что купить: ${productMock.titles.raw} или ${productMock.titles.raw}`,
                    },
                    pageObjects: {
                        pageMeta() {
                            return this.createPageObject(PageMeta);
                        },
                    },
                }),

                prepareSuite(PageDescriptionSuite, {
                    meta: {
                        environment: 'kadavr',
                        issue: 'MARKETFRONT-5153',
                        id: 'marketfront-3791',
                    },
                    params: {
                        expectedDescription: `В чем разница между ${productMock.titles.raw} ` +
                            `vs ${productMock.titles.raw}? Узнать разницу, сравнить цены и характеристики.`,
                    },
                    pageObjects: {
                        pageMeta() {
                            return this.createPageObject(PageMeta);
                        },
                    },
                })
            ),
        }
    ),
});
