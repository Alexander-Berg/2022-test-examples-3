import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import AnswerSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/answer-snippet';
import CategoryScrollSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/category-question/category-question-snippet';

import AnswerList from '@self/platform/spec/page-objects/components/Question/AnswersList';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import CategorySnippet from '@self/platform/spec/page-objects/components/CategoryQuestion/CategorySnippet';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'category-question',
    url: '/catalog--mobilnye-telefony/91491/question--kak-chasto-meniaete-telefony-raz-v-god/1563934',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                CategorySnippet.picture,
                ScrollBox.root,
            ],
            before(actions) {
                MainSuite.before(actions);
                hideElementBySelector(actions, AnswerList.root);
                hideElementBySelector(actions, '[data-zone-data*="статьи"]');
            },
        },
        {
            suiteName: 'JournalScrollBox',
            selector: '[data-zone-data*="статьи"]',
            ignore: [
                {every: 'picture'},
                {every: Counter.root},
            ],
            capture() {},
        },
        CategoryScrollSnippetSuite,
        AnswerSnippetSuite,
    ],
};
