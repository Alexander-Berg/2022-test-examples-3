import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import Price from '@self/platform/components/Price/__pageObject';


export default {
    suiteName: 'DefaultOffer',
    selector: DefaultOffer.root,
    ignore: [
        ShopRating.reviewsCountWrapper,
        {every: Price.root},
    ],
    capture() {},
};
