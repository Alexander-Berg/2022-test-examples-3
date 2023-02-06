import ProductReviewsScrollBox from '@self/platform/widgets/content/ProductReviewsScrollBox/__pageObject__';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'ProductReviewsScrollBoxMoreCard',
    selector: ProductReviewsScrollBox.moreCard,
    capture(actions) {
        // В скроллбоксе 3 отзыва
        hideElementBySelector(actions, ProductReviewsScrollBox.review);
        hideElementBySelector(actions, ProductReviewsScrollBox.review);
        hideElementBySelector(actions, ProductReviewsScrollBox.review);
    },
};
