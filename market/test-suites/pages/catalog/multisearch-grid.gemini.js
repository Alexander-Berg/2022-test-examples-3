import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import SearchSnippetCellSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCell';
import LegalInfoSuite from '@self/platform/spec/gemini/test-suites/blocks/legal';

import ClarifyCategory from '@self/root/src/widgets/content/search/Clarify/components/View/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideHeadBanner,
    hideLegalInfo,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

export default {
    suiteName: 'CatalogMultisearchGrid',
    url: {
        pathname: '/multisearch',
        query: {
            hid: [90783, 7811879, 13491296],
            gfilter: ['15086295:15086332', '14020987:14713996'],
            viewtype: 'grid',
        },
    },
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
                hideLegalInfo(actions);
                hideAllElementsBySelector(actions, SearchSnippetCellSuite.selector);
                disableAnimations(actions);
            },
            ignore: [
                ClarifyCategory.root,
            ],
        },
        {
            suiteName: 'ClarifyCategory',
            selector: ClarifyCategory.root,
            ignore: [
                {every: '[style*="background-image"]'},
            ],
            capture() {},
        },
        SearchSnippetCellSuite,
        LegalInfoSuite,
    ],
};
