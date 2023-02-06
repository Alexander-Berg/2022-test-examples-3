import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import FeaturePage from '@self/platform/spec/page-objects/Journal/FeaturedPages';
import JournalEntrypoint from '@self/platform/spec/page-objects/Journal/JournalEntrypoint';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';


export default {
    suiteName: 'JournalHubFamilyPage',
    url: '/journal/family',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, FeaturePage.root);
                MainSuite.before(actions);
            },
        },
        {
            suiteName: 'PageLayout',
            selector: FeaturePage.root,
            ignore: [
                {every: `${JournalEntrypoint.root} picture`},
                {every: `${JournalEntrypoint.root} img`},
                {every: Counter.root},
            ],
            capture() {},
        },
        {
            suiteName: 'SnippetBig',
            selector: JournalEntrypoint.root,
            ignore: [
                {every: Counter.root},
            ],
            capture() {},
        },
        {
            suiteName: 'SnippetMedium',
            selector: '[data-zone-name="entrypoint"]:nth-child(2)',
            ignore: [
                {every: Counter.root},
            ],
            capture() {},
        },
        {
            suiteName: 'SnippetSmall',
            selector: '[data-zone-name="entrypoint"]:nth-child(3)',
            ignore: [
                {every: Counter.root},
            ],
            capture() {},
        },
    ],
};
