import ProductReviewFormMicro from '@self/platform/spec/gemini/test-suites/blocks/ProductReviewFormMicro';
import {
    hideRegionPopup,
    hideModalFloat,
    hideHeader,
    hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

export default {
    suiteName: 'model-card-review-form-micro-stars',
    url: '/product--elektronnaia-kniga-digma-e635-4-gb/1952186490',
    before(actions) {
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideHeader(actions);
        hideHeadBanner(actions);
        initLazyWidgets(actions, 5000);
    },
    childSuites: [
        ProductReviewFormMicro,
    ],
};
