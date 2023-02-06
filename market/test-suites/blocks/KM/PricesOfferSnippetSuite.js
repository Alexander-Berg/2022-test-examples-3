import PricesOfferSnippet from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import Price from '@self/platform/components/Price/__pageObject';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMSnippetCardOffers',
    ignore: [
        {every: ShopRating.reviewsCountWrapper},
        {every: Price.root},
    ],
    before(actions) {
        hideProductTabs(actions);
    },
    selector: PricesOfferSnippet.root,
    capture() {},
};
