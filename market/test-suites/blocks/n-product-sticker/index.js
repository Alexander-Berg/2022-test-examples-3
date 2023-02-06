import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';

export default {
    suiteName: 'NavigationPanel',
    selector: ProductTabs.root,
    ignore: {every: ProductTabs.count},
    capture() {},
};
