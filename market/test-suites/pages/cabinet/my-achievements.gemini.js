import utils from '@yandex-market/gemini-extended-actions/';

import cookies from '@self/platform/constants/cookies';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';


export default {
    suiteName: 'MyAchievements',
    url: '/my/achievements',
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
        utils.authorize.call(actions, {
            login: profiles.reviewsfortest.login,
            password: profiles.reviewsfortest.password,
            url: '/my/achievements',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
