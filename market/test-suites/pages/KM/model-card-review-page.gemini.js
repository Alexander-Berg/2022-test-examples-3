import utils from '@yandex-market/gemini-extended-actions';

import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import {
    hideRegionPopup,
    hideModalFloat,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'KMReviewPage',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
    },
    childSuites: [
        {
            suiteName: 'WithCommentsUnauthorized',
            url: '/product/1971204201/reviews/83022936',
            selector: MainSuite.selector,
            capture() {
            },
        },
        {
            suiteName: 'WithCommentsAuthorized',
            url: '/product/1971204201/reviews/83022936',
            selector: MainSuite.selector,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.reviewsfortest.login,
                    password: profiles.reviewsfortest.password,
                    url: '/product/1971204201/reviews/83022936',
                });
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'WithoutComments',
            url: '/product/14164877/reviews/67843286',
            selector: MainSuite.selector,
            capture() {
            },
        },
    ],
};
