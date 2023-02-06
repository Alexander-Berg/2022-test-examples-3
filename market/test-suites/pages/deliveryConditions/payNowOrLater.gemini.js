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
    suiteName: 'PayNowOrLater',
    url: '/my/order/conditions?_filter=%2Fcontent%2Fcontent%2Fcontent%2F2%2F73692780-PayNowOrLater',
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