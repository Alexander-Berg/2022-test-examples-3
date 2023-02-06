import {setCookies} from '@yandex-market/gemini-extended-actions';
import cookies from '@self/platform/constants/cookie';

import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import AnswerList from '@self/platform/spec/page-objects/ProductAnswersList';
import AnswerSnippet from '@self/platform/components/Question/AnswerSnippet/__pageObject__';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';

import {hideRegionPopup, hideElementBySelector, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'CategoryQuestionPage',
    url: '/catalog--elektro-i-benzopily-tsepnye/15448926/question--kakoi-proizvoditel-samyi-nadezhnyi/1551970',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.AGGRESSIVE_SMART_BANNER_HIDDEN,
                value: '1',
            },
            {
                name: cookies.SIMPLE_SMART_BANNER_HIDDEN,
                value: '1',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A06%3A25.730008.png
            ...MainSuite,
            ignore: [
                {every: 'picture'},
                {every: Counter.root},
            ],
            before(actions) {
                initLazyWidgets(actions, 5000);
                hideElementBySelector(actions, AnswerList.root);
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A07%3A20.257752.jpg
            suiteName: 'AnswersHeader',
            selector: `${AnswerList.root} > div`,
            capture() {},
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A08%3A28.167957.jpg
            suiteName: 'Answer',
            selector: AnswerSnippet.root,
            capture() {},
        },
    ],
};
