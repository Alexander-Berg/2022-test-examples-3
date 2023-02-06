import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const ARTICLE_URL = '/my/articles/finish/209108';

export default {
    suiteName: 'ArticlesEditorFinishPage',
    // Нельзя открывать сразу /my/articles/finish/209108 - там вообще не грузится страница для незалогина
    url: '/blank',
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.authorCabinet.login,
            password: profiles.authorCabinet.password,
            url: ARTICLE_URL,
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        MainSuite,
    ],
};
