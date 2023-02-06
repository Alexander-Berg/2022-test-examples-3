import Main from '@self/platform/spec/page-objects/main';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import {hideRegionPopup, hideElementBySelector, hideHeader, hideHeadBanner} from '@self/platform/spec/gemini/helpers/hide';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import AnswerList from '@self/platform/spec/page-objects/ProductAnswersList';
import AnswerSnippet from '@self/platform/components/Question/AnswerSnippet/__pageObject__';
import AskForMore from '@self/platform/spec/page-objects/components/Questions/AskMoreFormWrapper';
import Footer from '@self/platform/spec/page-objects/Footer';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'QuestionPage',
    ignore: {every: Counter.root},
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'nHSBh',
                value: '1',
            },
        ]);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            suiteName: 'VendorAndUser',
            url: '/product/10545271/question/1520210',
            childSuites:
            [
                {
                    ...MainSuite,
                    before(actions) {
                        const selector = [
                            AnswerList.root,
                            AskForMore.root,
                        ].join(', ');
                        initLazyWidgets(actions, 5000);
                        new ClientAction(actions).removeElems(selector);
                        hideHeader(actions);
                        hideHeadBanner(actions);
                    },
                },
                {
                    suiteName: 'AnswerVendor',
                    selector: AnswerSnippet.root,
                    capture() {},
                },
                {
                    suiteName: 'AnswerUser',
                    selector: `${AnswerSnippet.root}:nth-child(2)`,
                    capture() {},
                },
                {
                    suiteName: 'AskForMore',
                    selector: `${AskForMore.root} > div`,
                    capture() {},
                },
            ],
        },
        {
            suiteName: 'Shop',
            url: '/product/1722992983/question/1636643',
            selector: Main.root,
            capture() {
            },
            before(actions) {
                hideElementBySelector(actions, Footer.root);
            },
        },
    ],
};
