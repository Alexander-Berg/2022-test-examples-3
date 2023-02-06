import {prepareSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import ProductReviewsScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReviewsScrollBox';
import ProductReviewsScrollBox from '@self/platform/widgets/content/ProductReviewsScrollBox/__pageObject__';

import {createUserReviewWithPhotos} from '@self/platform/spec/hermione/fixtures/review';

const reviewId = 500;

export default prepareSuite(ProductReviewsScrollBoxSuite, {
    hooks: {
        async beforeEach() {
            const testProductId = 1722193751;
            const testSlug = 'test-slug';
            const testUser = profiles.ugctest3;
            const reviewUserUid = testUser.uid;
            const testReview = {
                ...createUserReviewWithPhotos(testProductId, reviewUserUid),
                id: reviewId,
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

            await this.browser.yaOpenPage('touch:product', {
                productId: testProductId,
                slug: testSlug,
            });
            await this.browser.yaSlowlyScroll(ProductReviewsScrollBox.root);
        },
    },
    pageObjects: {
        productReviewsScrollBox() {
            return this.createPageObject(ProductReviewsScrollBox);
        },
    },
    params: {
        productId: 1722193751,
        slug: 'test-slug',
        reviewId,
    },
});
