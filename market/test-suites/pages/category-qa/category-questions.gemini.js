import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import CategoryScrollSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/category-question/category-question-snippet';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import CategorySnippet from '@self/platform/spec/page-objects/components/CategoryQuestion/CategorySnippet';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Caption from '@self/platform//spec/page-objects/widgets/parts/QuestionsLayout/QuestionList/Caption';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'category-questions',
    url: '/catalog--kholodilniki/15450081/questions',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: Counter.root},
                {every: `${ScrollBox.root} picture`},
                CategorySnippet.picture,
                ScrollBox.root,
                Caption.root,
            ],
        },
        {
            suiteName: 'QuestionNumber',
            selector: Caption.root,
            capture() {
            },
        },
        CategoryScrollSnippetSuite,
    ],
};
