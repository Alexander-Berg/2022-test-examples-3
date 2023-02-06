import ProductVertSnippet from '@self/platform/spec/page-objects/VertProductSnippet';

export default {
    suiteName: 'Discount',
    selector: '[data-zone-name="ScrollBox"]',
    ignore: [{every: ProductVertSnippet.root}],
    capture() {},
};
