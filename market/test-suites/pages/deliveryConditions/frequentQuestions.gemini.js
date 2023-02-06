import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

export default {
    suiteName: 'Order Conditions',
    url: '/my/order/conditions',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
