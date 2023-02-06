import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import SearchSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCard';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideAllElementsBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'Wishlist',
    url: '/my/wishlist',
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.ugctest3.login,
            password: profiles.ugctest3.password,
            url: '/my/wishlist',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideAllElementsBySelector(actions, 'article');
                MainSuite.before(actions);
            },
        },
        SearchSnippetSuite,
    ],
};
