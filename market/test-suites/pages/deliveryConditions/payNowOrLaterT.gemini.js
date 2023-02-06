import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {
    hideRegionPopup, hideModalFloat, hideParanja, hideMooa,
} from '@self/platform/spec/gemini/helpers/hide';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

export default {
    suiteName: 'PayNowOrLaterT',
    url: '/my/order/conditions?_filter=%2Fcontent%2Fcontent%2Fcontent%2F2%2F73692786-PayNowOrLater',
    selector: MainSuite.selector,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    capture() {},
};
