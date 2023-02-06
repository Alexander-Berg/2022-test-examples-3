import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const PAGE_URL = '/invalid-poll?agitationId=1-12345';

export default {
    suiteName: 'InvalidPoll',
    url: PAGE_URL,
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.reviewsfortest.login,
            password: profiles.reviewsfortest.password,
            url: PAGE_URL,
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
