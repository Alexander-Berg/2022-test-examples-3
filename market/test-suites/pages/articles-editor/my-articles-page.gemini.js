import utils from '@yandex-market/gemini-extended-actions';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ArticleSnippetPopupContent from '@self/platform/spec/page-objects/components/MyArticles/ArticleSnippet/PopupContent';
import ArticleSnippet from '@self/platform/spec/page-objects/components/MyArticles/ArticleSnippet';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'MyArticles',
    url: '/my/articles',
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: profiles.authorCabinet.login,
            password: profiles.authorCabinet.password,
            url: '/my/articles',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        MainSuite,
        {
            suiteName: 'Popup',
            selector: [
                ArticleSnippet.root,
                ArticleSnippetPopupContent.root,
            ],
            before(actions, find) {
                actions.click(find(ArticleSnippet.contextMenuButton));
                actions.wait(2000);
            },
            capture() {
            },
        },
    ],
};
