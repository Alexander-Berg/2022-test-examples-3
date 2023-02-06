import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import SearchSnippetCellSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCell';
import LegalInfoSuite from '@self/platform/spec/gemini/test-suites/blocks/legal';
import LogoCarouselSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/LogoCarousel';
import PremiumGallerySuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/SearchIncut';
import CategoryQuestionsFooterEntrypointSuite from
    '@self/platform/spec/gemini/test-suites/blocks/CategoryQuestionsFooterEntrypoint';

import CategoryQuestionsTopEntrypoint from '@self/platform/widgets/parts/CategoryQuestionsTopEntrypoint/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideCategoryQuestionsFooterEntrypoint,
    hideLegalInfo,
    hideHeadBanner,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {setCookies} from '@yandex-market/gemini-extended-actions';


export default {
    suiteName: 'CatalogGuruLightGrid',
    url: {
        pathname: '/catalog--septiki/56330/list',
        query: {
            viewtype: 'grid',
        },
    },
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'viewtype',
                value: 'grid',
            },
        ]);
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
        SearchSnippetCellSuite,
        CategoryQuestionsFooterEntrypointSuite,
        PremiumGallerySuite,
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
