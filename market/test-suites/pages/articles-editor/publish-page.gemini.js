import utils from '@yandex-market/gemini-extended-actions/';
import body from '@self/platform/spec/gemini/test-suites/blocks/body';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'ArticlesEditorPublishPage',
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.authorCabinet.login,
            password: profiles.authorCabinet.password,
            url: '/my/articles/publish/209108',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        body,
    ],
};
