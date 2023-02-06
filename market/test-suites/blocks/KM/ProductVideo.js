import ProductVideo from '@self/platform/spec/page-objects/ProductVideo';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMProductVideo',
    before(actions) {
        hideProductTabs(actions);
    },
    selector: ProductVideo.root,
    ignore: [
        ProductVideo.mainContent,
        {every: ProductVideo.itemInfo},
        {every: ProductVideo.itemVideo},
        {every: ProductVideo.itemText},
    ],
    capture() {},
};
