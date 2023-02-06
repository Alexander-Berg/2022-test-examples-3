import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import LimitedAutosizedTextarea from '@self/platform/spec/page-objects/components/ArticlesEditor/LimitedAutosizedTextarea';
import utils from '@yandex-market/gemini-extended-actions';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'JournalEditor',
    url: '/my/articles/edit',
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.authorCabinet.login,
            password: profiles.authorCabinet.password,
            url: '/my/articles/edit',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            capture(actions) {
                actions.waitForElementToShow(LimitedAutosizedTextarea.root, 5000);
            },
        },
    ],
};
