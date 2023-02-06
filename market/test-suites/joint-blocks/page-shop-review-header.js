import Breadcrumbs from '@self/platform/spec/page-objects/n-breadcrumbs';
import ShopHeadline from '@self/platform/spec/page-objects/n-shop-headline';

export default {
    suiteName: 'ReviewPageHeaders',
    selector: [Breadcrumbs.root, ShopHeadline.header],
    ignore: ShopHeadline.ratingDescription,
    capture() {},
};
