import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import FeedSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/BrandFeed';
import FeedSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/FeedSnippet';

import CustomTileSnippets from '@self/platform/widgets/content/CustomTileSnippets/__pageObject';
import DataSnippetReview from '@self/platform/spec/page-objects/DataSnippetReview';
import BrandMenu from '@self/platform/widgets/content/BrandZoneHeader/__pageObject';

import {hideRegionPopup, hideDevTools, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';


export default {
    suiteName: 'BrandRich',
    url: '/brands--vitek/152837',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        disableAnimations(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 5000);
                const selector = [CustomTileSnippets.root, '[data-zone-name="ScrollBox"]',
                    '[id="scroll-to-Отзывы"]', FeedSuite.selector, BrandMenu.root].join(', ');
                new ClientAction(actions).removeElems(selector);
                MainSuite.before(actions);
            },
            ignore: [
                {every: '[data-zone-name="MediaGallery"]'},
                {every: 'iframe'},
            ],
        },
        {
            suiteName: 'PopularCategories',
            selector: CustomTileSnippets.root,
            ignore: {every: '[data-zone-name="snippet"]'},
            before(actions) {
                hideElementBySelector(actions, BrandMenu.root);
            },
            capture() {},
        },
        {
            suiteName: 'Discounts',
            selector: FeedSuite.selector,
            ignore: {every: '[data-zone-name=snippet]'},
            capture() {},
            before(actions) {
                hideElementBySelector(actions, BrandMenu.root);
                initLazyWidgets(actions, 5000);
            },
        },
        {
            suiteName: 'DiscountSnippet',
            selector: `${FeedSuite.selector} [data-zone-name=snippet]`,
            before(actions) {
                hideElementBySelector(actions, BrandMenu.root);
            },
            capture() {},
        },
        {
            suiteName: 'Reviews',
            selector: '[data-zone-name="ScrollBox"]',
            ignore: {every: DataSnippetReview.root},
            capture() {},
            before(actions) {
                hideElementBySelector(actions, BrandMenu.root);
            },
        },
        {
            suiteName: 'ReviewSnippet',
            selector: DataSnippetReview.root,
            capture() {},
            before(actions) {
                hideElementBySelector(actions, BrandMenu.root);
            },
        },
        {
            ...FeedSuite,
            ignore: {every: '[data-zone-name="snippet"]'},
            before(actions) {
                // нам нужен второй такой блок
                hideElementBySelector(actions, FeedSuite.selector);
                hideElementBySelector(actions, BrandMenu.root);
            },
        },
        {
            suiteName: 'MenuRoot',
            selector: `${BrandMenu.root} > div > div`,
            capture() {},
            before(actions) {
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`
                document.querySelector('[id="scroll-to-Отзывы"]').scrollIntoView()`));
                actions.wait(1000);
            },
        },
        {
            ...FeedSnippetSuite,
            selector: '[data-zone-name="snippet"]',
            before(actions) {
                hideElementBySelector(actions, BrandMenu.root);
            },
        },
    ],
};
