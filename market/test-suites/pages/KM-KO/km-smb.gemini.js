import DefaultOfferSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/DefaultOffer';

import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'SMBonKM',
    url: '/product--biustgalter-minimal/744575179',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        DefaultOfferSuite,
    ],
};
