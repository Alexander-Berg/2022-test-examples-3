import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'MySettings',
    url: '/my/settings',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        utils.authorize.call(actions, {
            login: profiles.authorCabinet.login,
            password: profiles.authorCabinet.password,
            url: '/my/settings',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
