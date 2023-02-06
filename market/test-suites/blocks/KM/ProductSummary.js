import ProductSummary from '@self/platform/spec/page-objects/ProductSummary';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import Price from '@self/platform/components/Price/__pageObject';
import MorePricesLink from '@self/platform/widgets/content/MorePricesLink/__pageObject';

export default {
    suiteName: 'ProductSummary',
    selector: ProductSummary.root,
    ignore: [
        {every: ShopRating.root},
        {every: Price.root},
        MorePricesLink.root,
        '[data-zone-name="Upsale"]',
    ],
    capture() {},
};
