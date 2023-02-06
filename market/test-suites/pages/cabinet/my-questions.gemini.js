import utils from '@yandex-market/gemini-extended-actions/';

import cookies from '@self/platform/constants/cookies';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import UserQuestions from '@self/platform/widgets/content/UserQuestions/__pageObject/';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';


const QUESTIONS_URL = '/my/questions';

export default {
    suiteName: 'MyQuestions',
    url: '/my/questions',
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
            suiteName: 'UserWithQuestions',
            ignore: [
                {every: `${UserQuestions.root} picture`},
            ],
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.authorCabinet.login,
                    password: profiles.authorCabinet.password,
                    url: QUESTIONS_URL,
                });
                MainSuite.before(actions);
                actions.wait(1000); // избегаем пиксельной тряски
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {},
        },
        {
            ...MainSuite,
            suiteName: 'NoQuestions',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: QUESTIONS_URL,
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
