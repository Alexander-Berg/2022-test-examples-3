// suites
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
// pageObjects
import UgcVideoActivity from '@self/platform/components/UgcVideo/Activity/__pageObject';

import {hideModalFloat, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KMUgcVideo',
    url: '/product--kompiuternaia-garnitura-sven-ap-g888mv/13859021/video/927',
    ignore: [
        UgcVideoActivity.viewsCount,
        '[data-zone-name="product-snippet"] span',
        {every: 'iframe'},
    ],
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
