import Review from '@self/platform/spec/page-objects/components/ProductReview';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    // Хотим видеть определённый отзыв первым в списке, прокидываем гет-параметр
    suiteName: 'firstReviewId-Buying500grams',
    url: {
        pathname: '/product/168907088/reviews',
        query: {
            'no-tests': 1,
            firstReviewId: 93298324,
        },
    },
    selector: `${Review.root}:nth-child(1)`,
    capture() {},
    before(actions) {
        hideProductTabs(actions);
    },
};
