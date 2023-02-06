import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import ReviewFormMicroButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewFormMicro/button';
import ReviewFormMicroStarsSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewFormMicro/stars';
import ProductReviewFormMicro from '@self/platform/widgets/content/ProductReviewFormMicro/__pageObject__';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';

import {createUserReviewWithPhotos} from '@self/platform/spec/hermione/fixtures/review';


export default makeSuite('Микро-форма отзыва.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Если отзыв текущего пользователя содержит текст.', {
            story: prepareSuite(ReviewFormMicroButtonSuite, {
                hooks: {
                    async beforeEach() {
                        const testProductId = 1722193751;
                        const testSlug = 'test-slug';
                        const testUser = profiles.ugctest3;
                        const reviewUserUid = testUser.uid;
                        const testReview = {
                            ...createUserReviewWithPhotos(testProductId, reviewUserUid),
                            averageGrade: 4,
                        };
                        const testProduct = createProduct({
                            type: 'model',
                            categories: [{
                                id: 123,
                            }],
                            slug: testSlug,
                            deletedId: null,
                            prices: createPriceRange(300, 400, 'RUB'),
                        }, testProductId);

                        await this.browser
                            .setState('report', testProduct)
                            .setState('schema', {
                                gradesOpinions: [testReview],
                                modelOpinions: [testReview],
                            });

                        await this.browser.yaLogin(
                            testUser.login,
                            testUser.password
                        );

                        await this.browser.yaOpenPage('touch:product', {
                            productId: testProductId,
                            slug: testSlug,
                        });
                        await this.browser.yaSlowlyScroll(ProductReviewFormMicro.root);
                    },
                },
                pageObjects: {
                    productReviewFormMicro() {
                        return this.createPageObject(ProductReviewFormMicro);
                    },
                },
                params: {
                    productId: 1722193751,
                    slug: 'test-slug',
                    buttonText: 'Изменить мой отзыв',
                },
            }),
        }),
        makeSuite('Если отзыв текущего пользователя не содержит текст.', {
            story: prepareSuite(ReviewFormMicroButtonSuite, {
                hooks: {
                    async beforeEach() {
                        const testProductId = 1722193751;
                        const testSlug = 'test-slug';
                        const testUser = profiles.ugctest3;
                        const reviewUserUid = testUser.uid;
                        const testReview = {
                            ...createUserReviewWithPhotos(testProductId, reviewUserUid),
                            averageGrade: 4,
                            comment: '',
                            pro: '',
                            contra: '',
                        };
                        const testProduct = createProduct({
                            type: 'model',
                            categories: [{
                                id: 123,
                            }],
                            slug: testSlug,
                            deletedId: null,
                            prices: createPriceRange(300, 400, 'RUB'),
                        }, testProductId);

                        await this.browser
                            .setState('report', testProduct)
                            .setState('schema', {
                                gradesOpinions: [testReview],
                                modelOpinions: [testReview],
                            });

                        await this.browser.yaLogin(
                            testUser.login,
                            testUser.password
                        );

                        await this.browser.yaOpenPage('touch:product', {
                            productId: testProductId,
                            slug: testSlug,
                        });
                        await this.browser.yaSlowlyScroll(ProductReviewFormMicro.root);
                    },
                },
                pageObjects: {
                    productReviewFormMicro() {
                        return this.createPageObject(ProductReviewFormMicro);
                    },
                },
                params: {
                    productId: 1722193751,
                    slug: 'test-slug',
                    buttonText: 'Оставить отзыв',
                },
            }),
        }),
        makeSuite('Если текущий пользователь не оставлял отзыва', {
            story: prepareSuite(ReviewFormMicroStarsSuite, {
                hooks: {
                    async beforeEach() {
                        const testProductId = 1722193751;
                        const testSlug = 'test-slug';
                        const testUser = profiles.ugctest3;
                        const reviewUser = profiles.dzot61;
                        const reviewUserUid = reviewUser.uid;
                        const testReview = createUserReviewWithPhotos(testProductId, reviewUserUid);
                        const testProduct = createProduct({
                            type: 'model',
                            categories: [{
                                id: 123,
                            }],
                            slug: testSlug,
                            deletedId: null,
                            prices: createPriceRange(300, 400, 'RUB'),
                        }, testProductId);

                        await this.browser
                            .setState('report', testProduct)
                            .setState('schema', {
                                gradesOpinions: [testReview],
                                modelOpinions: [testReview],
                            });

                        await this.browser.yaLogin(
                            testUser.login,
                            testUser.password
                        );
                        await this.browser.yaOpenPage('touch:product', {
                            productId: testProductId,
                            slug: testSlug,
                        });
                        await this.browser.yaSlowlyScroll(ProductReviewFormMicro.root);
                    },
                },
                pageObjects: {
                    ratingInput() {
                        return this.createPageObject(RatingInput, {
                            root: ProductReviewFormMicro.root,
                        });
                    },
                },
                params: {
                    productId: 1722193751,
                    slug: 'test-slug',
                },
            }),
        })
    ),
});
