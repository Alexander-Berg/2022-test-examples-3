import ProductOverview from '@self/platform/spec/page-objects/Journal/ProductOverview';
import EntrypointArticleSnippet from '@self/platform/spec/page-objects/Journal/EntrypointArticleSnippet';
import LinkButton from '@self/platform/spec/page-objects/Journal/LinkButton';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalArticlesWidgets',
    url: '/journal/story/stili-interera-right-column',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            suiteName: 'EntrypointArticleSnippet journal widget',
            selector: EntrypointArticleSnippet.root,
            capture(actions) {
                actions.waitForElementToShow(EntrypointArticleSnippet.root, 5000);
            },
        },
        {
            suiteName: 'LinkButton journal widget',
            selector: LinkButton.root,
            before(actions) {
                setDefaultGeminiCookies(actions);
                hideDevTools(actions);
                hideRegionPopup(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(LinkButton.root, 50000);
            },
        },
        {
            suiteName: 'ProductOverview journal widget',
            selector: ProductOverview.root,
            capture(actions) {
                actions.waitForElementToShow(ProductOverview.root, 5000);
            },
        },
    ],
};
