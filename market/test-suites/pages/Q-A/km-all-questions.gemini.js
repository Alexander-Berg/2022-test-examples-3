import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import QuestionSnippet from '@self/platform/spec/page-objects/QuestionSnippet';
import AnswerSnippet from '@self/platform/components/Question/AnswerSnippet/__pageObject__';
import QuestionList from '@self/platform/spec/page-objects/QuestionList';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';

import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideHeader,
    hideHeadBanner,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KM-AllQuestions',
    url: '/product/8504043/questions',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        hideHeader(actions);
        hideHeadBanner(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: {every: Counter.root},
            before(actions) {
                initLazyWidgets(actions, 5000);
                hideElementBySelector(actions, QuestionList.root);
                hideElementBySelector(actions, ScrollBox.root);
                actions.wait(1000);
            },
        },
        {
            suiteName: 'QuestionSnippet',
            selector: QuestionSnippet.root,
            capture() {},

        },
        {
            suiteName: 'AnswerSnippet',
            selector: AnswerSnippet.root,
            capture() {},
        },
        {
            suiteName: 'ScrollBox',
            selector: ScrollBox.root,
            ignore: [
                {every: 'picture'},
            ],
            before(actions) {
                initLazyWidgets(actions, 5000);
                actions.wait(500); // боремся с тряской
            },
        },
    ],
};
