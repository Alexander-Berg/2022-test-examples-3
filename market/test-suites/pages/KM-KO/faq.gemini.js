import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'FAQ',
    url: {
        pathname: '/faq',
        query: {
            hid: 91491,
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
