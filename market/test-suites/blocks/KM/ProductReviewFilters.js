import ProductStat from '@self/platform/spec/page-objects/components/ProductRatingStat';
import ReviewsPhotoFilter from '@self/platform/spec/page-objects/components/ReviewsPhotoFilter';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMReviewFilters',
    before(actions) {
        hideProductTabs(actions);
    },
    selector: [
        ProductStat.root,
        ReviewsPhotoFilter.root,
    ],
    capture() {
    },
};
