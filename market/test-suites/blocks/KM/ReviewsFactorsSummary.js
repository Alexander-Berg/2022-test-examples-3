import ReviewSummary from '@self/platform/spec/page-objects/components/ReviewsFactorsSummary';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMReviewFactorsSummary',
    before(actions) {
        hideProductTabs(actions);
    },
    selector: ReviewSummary.root,
    capture() {},
};
