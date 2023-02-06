import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import utils from '@yandex-market/gemini-extended-actions';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {
    hideFooterOld,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'CompareEmpty',
    url: '/compare',
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.testemptyrewies.login,
            password: profiles.testemptyrewies.password,
            url: '/compare',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                MainSuite.before(actions);
                hideFooterOld(actions);
            },
        },
    ],
};
