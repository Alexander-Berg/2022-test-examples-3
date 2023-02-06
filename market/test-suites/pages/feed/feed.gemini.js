import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import {hideRegionPopup, hideModalFloat, hideHeadBanner, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import utils from '@yandex-market/gemini-extended-actions';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

// Параметр expirement "прибивает" контент, который мы увидим в ленте. Почти кадавризация со стороны DJ.
const FIXED_FEED_URL = '/feed?experiment=market_app_ugc_feed_example_set_production';

export default {
    suiteName: 'Feed',
    url: FIXED_FEED_URL,
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: 'Recomend2017',
            password: 'enakentij',
            url: FIXED_FEED_URL,
        });
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideHeadBanner(actions);
        initLazyWidgets(actions, 5000);
        disableAnimations(actions);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, '[data-zone-name="ugc-feed"]');
            },
        },
        // в тесте используются data-zone-name, а не пейджобжекты,
        // потому что с пейджобжектами не удалось победить проблему out of bounds
        {
            suiteName: 'ReviewFeedSnippet',
            selector: '[data-zone-name="review"]',
            capture() {},
        },
        {
            suiteName: 'VideoFeedSnippet',
            selector: '[data-zone-name="video"]',
            capture() {},
        },
        {
            suiteName: 'QuestionFeedSnippet',
            selector: '[data-zone-name="question"]',
            capture() {},
        },
    ],
};
