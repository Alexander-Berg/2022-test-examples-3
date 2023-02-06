import ShopReviewNew from '@self/platform/spec/page-objects/widgets/parts/ShopReviewNew';

export default {
    suiteName: 'ShopReviewNew',
    selector: ShopReviewNew.root,
    capture(actions) {
        actions.waitForElementToShow(ShopReviewNew.root, 1000);
    },
};
