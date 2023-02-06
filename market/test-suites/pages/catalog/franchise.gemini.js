import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {
    hideRegionPopup,
    hideDevTools,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

export default {
    suiteName: 'Franchise',
    url: '/franchise--am-niam/14022575',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 5000);
                MainSuite.before(actions);
            },
            capture() {
            },
        },
    ],
};
