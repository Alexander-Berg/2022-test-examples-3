import Review from '@self/platform/spec/page-objects/components/ProductReview';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMProductReview',
    before(actions) {
        hideProductTabs(actions);
    },
    selector: `${Review.root}:nth-child(1)`,
    capture() {},
};
