import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createUser} from '@yandex-market/kadavr/dist/helpers/entities/user';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';

import {MODEL_GRADE} from '@self/root/src/entities/agitation/constants';

// suites
import ReviewFormProductFieldsSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/ProductFields';
import ReviewFormProductFieldsSuiteWithoutSave
    from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/ProductFields/withoutSave';
import ReviewFormProductFieldsWithExtraTasksSuite
    from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/ProductFields/withExtraTasks';
// page-objects
import ProductReviewNew from '@self/platform/spec/page-objects/widgets/parts/ProductReviewNew';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';
// parts
import productReviewsPlusPayment from './productReviewsPlusPayment';

import {picture, product, categoryId} from './kadavrMocks';

const userUid = '636368980';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Новый отзыв" карточки модели.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Гуру-модель.', {
            environment: 'kadavr',
            story: mergeSuites(
                prepareSuite(ReviewFormProductFieldsSuite, {
                    hooks: {
                        async beforeEach() {
                            const {routeParams, mocks} = product.guru;
                            const productWithPicture = mergeReportState([mocks.report, picture]);
                            const gainExpertise = createGainExpertise(0, 5, userUid);

                            await this.browser.setState('schema', {
                                users: [createUser({id: userUid})],
                                productFactors: {
                                    [categoryId]: [
                                        {id: 742, title: 'Экран'},
                                        {id: 743, title: 'Камера'},
                                        {id: 744, title: 'Время автономной работы'},
                                        {id: 745, title: 'Объем памяти'},
                                        {id: 746, title: 'Производительность'},
                                    ],
                                },
                            });
                            await this.browser.setState('storage', {gainExpertise});
                            await this.browser.setState('report', productWithPicture);
                            await this.browser.yaProfile('ugctest3', 'market:product-reviews-add', {
                                productId: routeParams.productId,
                                slug: routeParams.slug,
                            });
                        },
                    },
                    params: {
                        productId: product.guru.routeParams.productId,
                        slug: product.guru.routeParams.slug,
                    },
                    pageObjects: {
                        productReviewNew() {
                            return this.createPageObject(ProductReviewNew);
                        },
                        ratingInput() {
                            return this.createPageObject(RatingInput);
                        },
                    },
                }),
                prepareSuite(ReviewFormProductFieldsWithExtraTasksSuite, {
                    hooks: {
                        async beforeEach() {
                            const {routeParams, mocks} = product.guru;
                            const productWithPicture = mergeReportState([mocks.report, picture]);
                            const gainExpertise = createGainExpertise(0, 5, userUid);

                            await this.browser.setState('schema', {
                                users: [createUser({id: userUid})],
                                productFactors: {[categoryId]: []},
                                agitation: {
                                    [userUid]: [1, 2].map((_, index) => ({
                                        id: `${MODEL_GRADE}-123${index}`,
                                        entityId: String(index + 1),
                                        type: MODEL_GRADE,
                                    })),
                                },
                            });
                            await this.browser.setState('storage', {gainExpertise});
                            await this.browser.setState('report', productWithPicture);
                            await this.browser.yaProfile('ugctest3', 'market:product-reviews-add', {
                                productId: routeParams.productId,
                                slug: routeParams.slug,
                            });
                        },
                    },
                    params: {
                        productId: product.guru.routeParams.productId,
                        slug: product.guru.routeParams.slug,
                    },
                    pageObjects: {
                        productReviewNew() {
                            return this.createPageObject(ProductReviewNew);
                        },
                        ratingInput() {
                            return this.createPageObject(RatingInput);
                        },
                    },
                }),
                prepareSuite(ReviewFormProductFieldsSuiteWithoutSave, {
                    hooks: {
                        async beforeEach() {
                            const {routeParams, mocks} = product.guru;
                            const productWithPicture = mergeReportState([mocks.report, picture]);

                            await this.browser.setState('schema', {
                                users: [createUser({id: userUid})],
                            });
                            await this.browser.setState('report', productWithPicture);
                            await this.browser.yaProfile('ugctest3', 'market:product-reviews-add', {
                                productId: routeParams.productId,
                                slug: routeParams.slug,
                            });
                        },
                    },
                    params: {
                        productId: product.guru.routeParams.productId,
                        slug: product.guru.routeParams.slug,
                    },
                    pageObjects: {
                        productReviewNew() {
                            return this.createPageObject(ProductReviewNew);
                        },
                        ratingInput() {
                            return this.createPageObject(RatingInput);
                        },
                    },
                })
            ),
        }),
        productReviewsPlusPayment
    ),
});
