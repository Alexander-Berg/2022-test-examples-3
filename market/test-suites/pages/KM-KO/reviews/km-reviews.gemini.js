import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ReviewSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductReview';
import ReviewProductFilterSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductReviewFilters';
import ReviewProductSummary from '@self/platform/spec/gemini/test-suites/blocks/KM/ReviewsFactorsSummary';
import ReviewFirstReviewId from '@self/platform/spec/gemini/test-suites/blocks/KM/first-review-id';
import JournalScrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/km-journal-scrollbox';
import ProductHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductTitle';
import ProductVideoSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductVideo';


import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideProductTabs,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


import Review from '@self/platform/spec/page-objects/components/ProductReview';
import ProductStat from '@self/platform/spec/page-objects/components/ProductRatingStat';
import ReviewsPhotoFilter from '@self/platform/spec/page-objects/components/ReviewsPhotoFilter';
import ReviewSummary from '@self/platform/spec/page-objects/components/ReviewsFactorsSummary';
import TopOffersSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/TopOffers';


export default {
    suiteName: 'KM-reviews',
    url: '/product--noutbuk-asus-vivobook-15-x512/518204228/reviews',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                ProductStat.root,
                ReviewsPhotoFilter.root,
                ReviewSummary.root,
                {every: '[data-zone-name="media-preview"]'},
            ],
            before(actions) {
                initLazyWidgets(actions);
                const selector = [
                    Review.root, // ReviewSuite
                    TopOffersSuite.selector,
                    ProductVideoSuite.selector,
                    ProductHeadlineSuite.selector,
                    JournalScrollboxSuite.selector,
                ].join();
                new ClientAction(actions).removeElems(selector);
                MainSuite.before(actions);
            },
        },
        ReviewSuite,
        ReviewProductFilterSuite,
        ReviewProductSummary,
        {
            ...TopOffersSuite,
            url: '/product--stiralnaia-mashina-samsung-wf8590nlw9/13202482/reviews',
        },
        {
            ...ProductVideoSuite,
            url: '/product--stiralnaia-mashina-samsung-wf8590nlw9/13202482/reviews',
        },
        ReviewFirstReviewId,
        ProductHeadlineSuite,
        {
            ...ReviewSuite,
            suiteName: 'KMModificationReview',
            url: '/product/10581406/reviews', // Модификация группы шин
        },
        JournalScrollboxSuite,
    ],
};
