import OfferPage from '@self/platform/widgets/pages/OfferPage/__pageObject';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';


export default {
    suiteName: 'KOSummary',
    selector: OfferPage.summary,
    ignore: ShopRating.root,
    capture() {},
};
