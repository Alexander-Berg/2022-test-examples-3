import ProductTitle from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import ReviewsCount from '@self/platform/components/PageCardTitle/ReviewsCount/__pageObject';

export default {
    suiteName: 'KMProductTitle',
    selector: ProductTitle.root,
    ignore: ReviewsCount.root,
    capture() {},
};
