import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import LegalInfoSuite from '@self/platform/spec/gemini/test-suites/blocks/legal';
import SearchSnippetCellSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCell';

import ClarifyCategory from '@self/root/src/widgets/content/search/Clarify/components/View/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideHeadBanner,
    hideLegalInfo,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';


import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

export default {
    suiteName: 'CatalogSearchGrid',
    url: {
        pathname: '/search',
        query: {
            cvredirect: 2,
            text: 'red',
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
