import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {createShopInfo, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import ShopHeadlineSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopReviews/headline';
import RatingSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopReviews/rating';
import TitleSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopReviewsToolbar/title';
import ReviewsAddButtonSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopReviews/addButton';

// page-objects
import ShopReviews from '@self/platform/spec/page-objects/widgets/content/ShopReviews';
import ReviewsShopRatingSummary
    from '@self/platform/widgets/content/ShopReviews/components/ShopRatingSummary/__pageObject';
import ReviewsToolbar from '@self/platform/spec/page-objects/widgets/content/ShopReviewsToolbar';

import {offer, offerId} from '../fixtures/offerWithoutModel';
import {shopReview, user} from '../fixtures/shopReview';

export default makeSuite('Контент вкладки "Отзывы"', {
    id: 'marketfront-3486',
    issue: 'MARKETVERSTKA-34568',
    story: mergeSuites(
        {
            async beforeEach() {
                const state = mergeState([
                    offer,
                    createShopInfo(shopReview, shopReview.id),
                ]);
                await this.browser.setState('report', state);
                await this.browser.setState('schema', {
                    users: [user],
                    modelOpinions: [shopReview],
                });
                await this.browser.yaOpenPage('market:offer-reviews', {offerId});
            },
        },
        prepareSuite(ShopHeadlineSuite, {
            params: {
                expectedText: 'Отзывы о магазине test.yandex.ru',
            },
            pageObjects: {
                shopReviews() {
                    return this.createPageObject(ShopReviews);
                },
            },
        }),
        prepareSuite(RatingSuite, {
            pageObjects: {
                reviewsShopRatingSummary() {
                    return this.createPageObject(ReviewsShopRatingSummary);
                },
            },
        }),
        prepareSuite(TitleSuite, {
            pageObjects: {
                reviewsToolbar() {
                    return this.createPageObject(ReviewsToolbar);
                },
            },
        }),
        prepareSuite(ReviewsAddButtonSuite, {
            pageObjects: {
                shopReviews() {
                    return this.createPageObject(ShopReviews);
                },
            },
        })
    ),
});
