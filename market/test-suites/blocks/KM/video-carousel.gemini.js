import ProductPage from '@self/platform/spec/page-objects/ProductPage';

export default {
    suiteName: 'VideoCarousel',
    selector: ProductPage.videoCarousel,
    ignore: [{every: `${ProductPage.videoCarousel} img`}],
    capture() {},
};
