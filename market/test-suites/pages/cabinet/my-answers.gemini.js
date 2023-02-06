import utils from '@yandex-market/gemini-extended-actions/';

import cookies from '@self/platform/constants/cookies';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import UserAnswers from '@self/platform/widgets/content/UserAnswers/__pageObject/';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const ANSWERS_URL = '/my/answers';

export default {
    suiteName: 'MyAnswers',
    url: ANSWERS_URL,
    before(actions) {
        utils.setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.USER_ACHIEVEMENTS_ONBOARDING_COOKIE,
                value: '1',
            },
            {
                name: cookies.LKOB_COOKIE,
                value: '1',
            },
        ]);
    },
    childSuites: [
        {
            ...MainSuite,
            suiteName: 'UserWithAnswers',
            ignore: `${UserAnswers.root} picture`,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.authorCabinet.login,
                    password: profiles.authorCabinet.password,
                    url: ANSWERS_URL,
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
        {
            ...MainSuite,
            suiteName: 'NoAnswers',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: ANSWERS_URL,
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
    ],
};
