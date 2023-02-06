import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import ReviewFormProductSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ProductReviewForm';
import ReviewFormProductOnlyRatingSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ProductReviewForm/onlyRating';
import ReviewFormProductPhotoAttachedSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ProductReviewForm/photoAttached';
import ReviewFormProductAverageGradeParamSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ProductReviewForm/averageGradeParam';
// page-objects
import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import UserReview from '@self/platform/components/UserReview/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница оставления отзыва на товар.', {
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                reviewForm: () => this.createPageObject(ProductReviewForm),
                reviewItem: () => this.createPageObject(UserReview),
            });
        },

        'Авторизованный пользователь.': mergeSuites(
            {
                async beforeEach() {
                    const {productId, slug} = routes.reviews.product.add;
                    const categoryId = 91491;
                    const product = createProduct({
                        type: 'model',
                        deletedId: null,
                        categories: [
                            {
                                entity: 'category',
                                id: categoryId,
                                slug: 'mobilnye-telefony',
                                name: 'Мобильные телефоны',
                                fullName: 'Мобильные телефоны',
                                type: 'guru',
                                isLeaf: true,
                            },
                        ],
                        slug,
                    }, productId);
                    const users = [createUser({
                        ...profiles['pan-topinambur'],
                        uid: {
                            value: profiles['pan-topinambur'].uid,
                        },
                        id: profiles['pan-topinambur'].uid,
                    })];
                    await this.browser.setState('schema', {
                        users,
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
                    await this.browser.setState('report', product);
                    return this.browser.yaProfile('pan-topinambur', 'market:product-reviews-add', {
                        productId,
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },

            prepareSuite(ReviewFormProductSuite),
            prepareSuite(ReviewFormProductOnlyRatingSuite, {params: {
                productId: routes.reviews.product.add.productId,
                slug: routes.reviews.product.add.slug,
            }}),
            prepareSuite(ReviewFormProductPhotoAttachedSuite)
        ),

        'Неавторизованный пользователь': mergeSuites(
            {
                async beforeEach() {
                    const {productId} = routes.reviews.product.add;
                    const product = createProduct({
                        type: 'model',
                        deletedId: null,
                        categories: [
                            {
                                entity: 'category',
                                id: 91491,
                                slug: 'mobilnye-telefony',
                                name: 'Мобильные телефоны',
                                fullName: 'Мобильные телефоны',
                                type: 'guru',
                                isLeaf: true,
                            },
                        ],
                        slug: 'product',
                    }, productId);

                    await this.browser.setState('report', product);

                    return this.browser.yaProfile('pan-topinambur', 'market:product-reviews-add', {
                        productId,
                        averageGrade: 4,
                    });
                },
            },
            prepareSuite(ReviewFormProductAverageGradeParamSuite, {
                params: {
                    expectedAverageGrade: 4,
                },
            })
        ),
    },
});
