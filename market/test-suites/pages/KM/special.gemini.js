// suites
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import {hideModalFloat, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KMSpecial',
    url: '/product--smartfon-samsung-galaxy-a10/419572807/special',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
