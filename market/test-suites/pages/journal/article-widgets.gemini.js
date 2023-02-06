import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideHeadBanner,
    hideHeader,
} from '@self/platform/spec/gemini/helpers/hide';
import CommentariesView from '@self/platform/spec/page-objects/widgets/content/Commentaries/view';
import EntrypointArticleSnippet from '@self/platform/spec/page-objects/Journal/EntrypointArticleSnippet';
import ProductOverview from '@self/platform/spec/page-objects/Journal/ProductOverview';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import MaterialFooterSuite from '@self/platform/spec/gemini/test-suites/blocks/journal/material-footer.gemini';
import LinkButton from '@self/platform/spec/page-objects/Journal/LinkButton';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalArticleWidgets',
    url: '/journal/story/stili-interera-right-column',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        // Скрываем хэдэр, т.к. тень от него приводит к пиксельной тряске
        hideHeadBanner(actions);
        hideHeader(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: EntrypointArticleSnippet.root},
                {every: Counter.root},
            ],
            before(actions) {
                const selector = [
                    CommentariesView.root,
                    ProductOverview.root,
                    MaterialFooterSuite.selector,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        {
            suiteName: 'ArticleComment',
            selector: CommentariesView.root,
            capture() {},
        },
        {
            suiteName: 'EntrypointSnippet',
            selector: EntrypointArticleSnippet.root,
            capture(actions) {
                actions.waitForElementToShow(EntrypointArticleSnippet.root, 5000);
            },

        },
        {
            suiteName: 'ArticleLinkButton',
            selector: LinkButton.root,
            capture(actions) {
                actions.waitForElementToShow(LinkButton.root, 5000);
            },
        },
        {
            suiteName: 'ArticleFooterMaterials',
            url: '/journal/story/test-suites-journal-article-footer',
            childSuites: [
                MaterialFooterSuite,
            ],
        },
        {
            suiteName: 'ProductOverview',
            selector: ProductOverview.root,
            ignore: {every: `${ProductOverview.price}, ${ProductOverview.rating}`},
            capture(actions) {
                actions.waitForElementToShow(ProductOverview.root, 5000);
            },
        },
    ],
};
