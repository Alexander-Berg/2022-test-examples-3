import ProductTitle from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import QuestionsInfo from '@self/platform/components/PageCardTitle/QuestionsInfo/__pageObject';
import ReviewsCount from '@self/platform/components/PageCardTitle/ReviewsCount/__pageObject';
import Price from '@self/platform/components/Price/__pageObject';

export default {
    suiteName: 'KMProductHeadline',
    selector: ProductTitle.root,
    ignore: [
        ReviewsCount.root,
        QuestionsInfo.root,
        Price.root,
    ],
    capture() {},
};
