import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import MaterialFooterSuite from '@self/platform/spec/gemini/test-suites/blocks/journal/material-footer';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Price from '@self/platform/components/Price/__pageObject/';
import MaterialHeader from '@self/platform/spec/page-objects/Journal/MaterialHeader';
import Subscription from '@self/platform/spec/page-objects/Subscription';
import TableOfContents from '@self/platform/spec/page-objects/TableOfContents';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'JournalGoodsstoryArticle',
    url: '/journal/goodsstory/chto-podarit-fanatu-zvezdnyh-vojn',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: Counter.root},
                {every: ScrollBox.root},
            ],

            before(actions) {
                MainSuite.before(actions);
                const selector = [MaterialHeader.root, Subscription.root,
                    TableOfContents.root, MaterialFooterSuite.selector].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        {
            suiteName: 'Header',
            selector: MaterialHeader.root,
            ignore: [
                {every: Counter.root},
            ],
            capture() {},
        },
        {
            suiteName: 'Subscription',
            selector: Subscription.root,
            capture() {},
        },
        MaterialFooterSuite,
        {
            suiteName: 'TableOfContents',
            selector: TableOfContents.root,
            capture() {},
        },
        {
            suiteName: 'ProductSnippet',
            selector: ScrollBox.root,
            ignore: [
                {every: Price.root},
            ],
            before(actions) {
                hideElementBySelector(actions, ScrollBox.root);
            },
            capture() {},
        },
        {
            suiteName: 'ScrollBox',
            selector: '[data-zone-data*="RelevantPages"]',
            ignore: [
                {every: Counter.root},
                {every: 'picture'},
            ],
            capture() {},
        },
    ],
};
