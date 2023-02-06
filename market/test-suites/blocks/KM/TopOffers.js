import TopOffers from '@self/platform/spec/page-objects/widgets/content/TopOffers';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';

export default {
    suiteName: 'Top-6',
    selector: [
        TopOffers.viewRoot,
    ],
    ignore: [
        {every: TopOfferSnippet.itemPrice},
        {every: ShopRating.root},
        {every: '[data-zone-name="more-offers-link"]'},
    ],
    capture() {},
};
