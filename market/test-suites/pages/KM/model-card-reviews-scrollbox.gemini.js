import ProductReviewsScrollBox from '@self/platform/spec/gemini/test-suites/blocks/ProductReviewsScrollBox';
import ProductReviewsScrollBoxReview from '@self/platform/spec/gemini/test-suites/blocks/ProductReviewsScrollBox/review';
import ProductReviewsScrollMoreCard
    from '@self/platform/spec/gemini/test-suites/blocks/ProductReviewsScrollBox/moreCard';
import {
    hideRegionPopup,
    hideModalFloat,
    hideHeader,
    hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

export default {
    suiteName: 'model-card-reviews-scrollbox',
    url: '/product--elektronnaia-kniga-digma-e635-4-gb/1952186490',
    before(actions) {
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideHeader(actions);
        hideHeadBanner(actions);
        initLazyWidgets(actions, 5000);
    },
    childSuites: [
        ProductReviewsScrollBox,
        ProductReviewsScrollBoxReview,
        ProductReviewsScrollMoreCard,
    ],
};
