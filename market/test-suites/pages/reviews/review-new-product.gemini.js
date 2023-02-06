import ProductReviewNew from '@self/platform/spec/page-objects/widgets/parts/ProductReviewNew';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'ReviewNewProduct',
    url: '/product--smartfon-samsung-galaxy-s8/1722193751/reviews/add',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [{
        suiteName: 'ReviewFormScreen',
        selector: ProductReviewNew.root,
        capture(actions) {
            actions.waitForElementToShow(ProductReviewNew.root, 1000);
        },
    }],
};
