import ProductProperties from '@self/platform/spec/page-objects/n-w-product-recipes';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMProductRecipes',
    before(actions) {
        hideProductTabs(actions);
    },
    selector: ProductProperties.root,
    capture() {},
};
