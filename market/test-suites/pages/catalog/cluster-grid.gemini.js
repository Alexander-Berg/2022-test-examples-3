import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import SearchSnippetCellSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCell';
import LegalInfoSuite from '@self/platform/spec/gemini/test-suites/blocks/legal';
import SearchSnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
import LogoCarouselSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/LogoCarousel';

import CategoryQuestionsFooterEntrypointSuite from '@self/platform/spec/gemini/test-suites/blocks/CategoryQuestionsFooterEntrypoint';
import CategoryQuestionsTopEntrypoint from '@self/platform/widgets/parts/CategoryQuestionsTopEntrypoint/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideCategoryQuestionsFooterEntrypoint,
    hideLegalInfo,
    hideAllElementsBySelector, hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

const GRID_URL = '/catalog--platia-i-sarafany/57297/list?hid=7811901&cpa=1';

export default {
    suiteName: 'CatalogClusterGrid',
    url: GRID_URL,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 5000);
                // Из-за виртуализации надо скроллить вниз страницы чтобы в дереве были все элементы выдачи
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`document.querySelector('${LegalInfoSuite.selector}').scrollIntoView()`));
                hideHeadBanner(actions);
                hideCategoryQuestionsFooterEntrypoint(actions);
                hideLegalInfo(actions);
                hideAllElementsBySelector(actions, SearchSnippetCellSuite.selector);
                hideAllElementsBySelector(actions, LogoCarouselSuite.selector);
                hideAllElementsBySelector(actions, CategoryQuestionsFooterEntrypointSuite.selector);
                disableAnimations(actions);
            },
            ignore: [
                // Непостоянное число вопросов на категорию
                `${CategoryQuestionsTopEntrypoint.root} span > span:nth-child(2)`,
            ],
        },
        {
            ...SearchSnippetCellSuite,
            selector: [
                `${SearchSnippetCell.root}:nth-of-type(1)`,
                `${SearchSnippetCell.root}:nth-of-type(3)`,
            ],
        },
        CategoryQuestionsFooterEntrypointSuite,
        LegalInfoSuite,
        {
            ...LogoCarouselSuite,
            before(actions) {
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(`document.querySelector('${LogoCarouselSuite.selector}').scrollIntoView()`));
                actions.waitForElementToShow(`${LogoCarouselSuite.selector} img`, 4000);
            },
        },
    ],
};
