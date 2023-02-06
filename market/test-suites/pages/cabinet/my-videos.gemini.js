import utils from '@yandex-market/gemini-extended-actions/';
import cookies from '@self/platform/constants/cookies';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const VIDEOS_URL = '/my/videos';

export default {
    suiteName: 'MyVideos',
    url: VIDEOS_URL,
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
            suiteName: 'UserWithVideos',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.authorCabinet.login,
                    password: profiles.authorCabinet.password,
                    url: VIDEOS_URL,
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
            suiteName: 'NoVideos',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: VIDEOS_URL,
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
