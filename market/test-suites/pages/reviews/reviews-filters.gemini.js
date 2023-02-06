import ReviewsFilters from '@self/platform/spec/page-objects/widgets/parts/ReviewsFilters';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'ReviewsFilters',
    url: '/product--pylesos-shivaki-svc-1748/13795021/reviews/filters',
    selector: [ReviewsFilters.root, ReviewsFilters.applyButton],
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    capture() {},
};
