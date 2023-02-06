import cookies from '@self/platform/constants/cookies';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

import QuestionAgitations from '@self/platform/widgets/content/QuestionAgitations/__pageObject';

import utils from '@yandex-market/gemini-extended-actions/';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const TASKS_URL = '/my/tasks';

export default {
    suiteName: 'MyTasks',
    url: TASKS_URL,
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
            suiteName: 'EmptyCabinet',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: TASKS_URL,
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
            suiteName: 'UserWithTasks',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.authorCabinet.login,
                    password: profiles.authorCabinet.password,
                    url: TASKS_URL,
                });
                MainSuite.before(actions);
                // Убираем примеры вопросов совсем из-за плавающей высоты сниппета
                new ClientAction(actions).removeElems(QuestionAgitations.snippet);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
    ],
};
