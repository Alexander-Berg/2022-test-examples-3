import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import BrandHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/Headline';
import RubricatorSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/Rubricator';
import RubricatorSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/RubricatorSnippet';
import FeedSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/BrandFeed';
import DiscountSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/Discount';
import SeeAlsoSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/SeeAlso';
import AboutSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/About';
import FeedSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/FeedSnippet';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';


export default {
    suiteName: 'BrandFree',
    url: '/brands--sony/152955',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                const selector = [BrandHeadlineSuite.selector,
                    RubricatorSuite.selector,
                    FeedSuite.selector,
                    SeeAlsoSuite.selector,
                    DiscountSuite.selector,
                    AboutSuite.selector].join(', ');
                new ClientAction(actions).removeElems(selector);
                MainSuite.before(actions);
            },
        },
        BrandHeadlineSuite,
        RubricatorSuite,
        RubricatorSnippetSuite,
        FeedSuite,
        FeedSnippetSuite,
        DiscountSuite,
        SeeAlsoSuite,
        AboutSuite,
    ],
};
