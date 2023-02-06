import ShopReviewNew from '@self/platform/spec/gemini/test-suites/blocks/widgets/parts/ShopReviewNew.gemini';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'ShopReviewsNewPage',
    url: '/shop/56191/reviews/add',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        ShopReviewNew,
    ],
};
