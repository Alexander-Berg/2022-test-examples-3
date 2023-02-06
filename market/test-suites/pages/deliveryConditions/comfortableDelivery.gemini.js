import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {
    hideRegionPopup,
    hideHeadBanner,
    hideDevTools,
    hideHeader2,
    hideYndxBug,
} from '@self/platform/spec/gemini/helpers/hide';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

export default {
    suiteName: 'ComfortableDelivery',
    url: '/my/order/conditions?_filter=%2Fcontent%2Fcontent%2Fcontent%2F1%2F73692766-ComfortableDelivery',
    selector: MainSuite.selector,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideHeader2(actions);
        hideHeadBanner(actions);
        hideDevTools(actions);
        hideYndxBug(actions);
    },
    capture() {},
};
