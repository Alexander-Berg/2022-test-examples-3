import ProductReviewsList from '@self/platform/spec/page-objects/components/ProductReviewsList';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KM-reviews-comments',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        {
            suiteName: 'km-reviews-comments-users',
            url: {
                pathname: '/product--telefon-nokia-8110-4g/1969020910/reviews',
                query: {
                    page: '12',
                    sort_by: 'date',
                },
            },
            selector: ProductReviewsList.root,
            capture() {
            },
        },
        {
            suiteName: 'km-reviews-comments-vendors',
            url: '/product--ventiliatsionnaia-ustanovka-royal-clima-soffio-rcs-950-2-0/1721638221/reviews',
            selector: ProductReviewsList.root,
            capture() {
            },
        },
    ],
};
