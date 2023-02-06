import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import CategoryQuestionsFooterEntrypointSuite from '@self/platform/spec/gemini/test-suites/blocks/CategoryQuestionsFooterEntrypoint';
import LogoCarouselSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/LogoCarousel';
import LegalInfoSuite from '@self/platform/spec/gemini/test-suites/blocks/legal';
import ReviewSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/reviewSnippet';

import Image from '@self/platform/spec/page-objects/image';
import CategoryQuestionsTopEntrypoint from '@self/platform/widgets/parts/CategoryQuestionsTopEntrypoint/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideElementBySelector,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';

import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'HubReviews',
    url: '/catalog--kholodilniki/71639/list?show-reviews=1',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                // Из-за виртуализации надо скроллить вниз страницы чтобы в дереве были все элементы выдачи
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`document.querySelector('${LegalInfoSuite.selector}').scrollIntoView()`));
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`document.querySelector('${LegalInfoSuite.selector}').scrollIntoView()`));
                initLazyWidgets(actions, 5000);
                hideElementBySelector(actions, LogoCarouselSuite.selector);
                hideAllElementsBySelector(actions, ReviewSnippetSuite.selector);
                MainSuite.before(actions);
            },
            ignore: [
                {every: Image.root},
                // Блок Q&A в футере, на него отдельный сьют
                CategoryQuestionsFooterEntrypointSuite.selector,
                // Непостоянное число вопросов на категорию
                `${CategoryQuestionsTopEntrypoint.root} span > span:nth-child(2)`,
            ],
        },
        ReviewSnippetSuite,
        CategoryQuestionsFooterEntrypointSuite,
        {
            ...LogoCarouselSuite,
            before(actions) {
                // Из-за виртуализации надо скроллить вниз страницы чтобы стриггерить загрузку виджета
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`document.querySelector('${LegalInfoSuite.selector}').scrollIntoView()`));
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`document.querySelector('${LegalInfoSuite.selector}').scrollIntoView()`));
                actions.wait(6000);
            },
        },
    ],
};
