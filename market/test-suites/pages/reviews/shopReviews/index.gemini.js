import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ShopReview from '@self/platform/components/ShopReview/Review/__pageObject';
import ShopReviewList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';
import ShopReviewUserList from '@self/platform/widgets/content/ShopReviewsUserList/__pageObject';
import LastViewedOffer from '@self/platform/widgets/content/ShopReviews/components/LastViewedOffer/__pageObject';
import ShopRatingStat from '@self/platform/widgets/content/ShopRatingStat/__pageObject__';
import {offers} from '@self/project/src/spec/gemini/configs/offers';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {
    hideScrollbar,
    hideElementBySelector,
    hideHeader2,
    hideFooter,
    hideTopmenu,
} from '@self/platform/spec/gemini/helpers/hide';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import utils from '@yandex-market/gemini-extended-actions/';


export default {
    suiteName: 'ReviewShopPage',
    url: {
        pathname: 'shop/774/reviews',
        query: {
            sort_desc: 'asc',
            sort_by: 'date',
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideScrollbar(actions);
        hideHeader2(actions);
        hideFooter(actions);
        hideTopmenu(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: ShopRatingStat.reviewItemCount},
                LastViewedOffer.root,
            ],
            before(actions) {
                MainSuite.before(actions);
                hideElementBySelector(actions, ShopReviewList.root);
            },
        },
        {
            // Хотим видеть определённый отзыв первым в списке, прокидываем гет-параметр
            suiteName: 'firstReviewId-test, Darya Komasutskaya review',
            url: {
                pathname: '/shop/774/reviews',
                query: {
                    firstReviewId: 18134793,
                },
            },
            selector: ShopReview.root,
            before(actions) {
                // Ждём окончания автоподскролла
                actions.wait(1000);
            },
            capture() {},
        },
        {
            suiteName: 'MyReview',
            selector: ShopReviewUserList.root,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.reviewsfortest.login,
                    password: profiles.reviewsfortest.password,
                    url: 'shop/99972/reviews',
                });
            },
            capture() {},
            after(actions) {
                utils.logout.call(actions);
            },
        },
        {
            suiteName: 'ReviewsLastShopOffer',
            url: {
                pathname: `shop/${offers.offerWithLinkedKM.shopid}/reviews`,
                query: {
                    cmid: offers.offerWithLinkedKM.wareid,
                },
            },
            selector: LastViewedOffer.root,
            ignore: [
                '[data-zone-name="reviews-count"]',
                {every: LastViewedOffer.price},
            ],
            capture(actions) {
                actions.waitForElementToShow(LastViewedOffer.root, 5000);
            },
        },
    ],
};
