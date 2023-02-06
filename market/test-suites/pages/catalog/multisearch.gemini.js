import deleteCookie from '@yandex-market/gemini-extended-actions/actions/deleteCookie';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import ClarifyCategorySuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/category.gemini';
import SearchResults from '@self/platform/spec/gemini/test-suites/blocks/catalog/snippet.gemini';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import ClarifyCategory from '@self/platform/spec/page-objects/ClarifyingCategories';
import SearchResultList from '@self/platform/spec/page-objects/SearchResults';
import {hideScrollbar, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Multisearch',
    url: {
        pathname: '/multisearch',
        query: {
            hid: [90783, 7811879, 13491296],
            gfilter: ['15086295:15086332', '14020987:14713996'],
            lr: 12,
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');

        new ClientAction(actions).removeElems(selector);
        hideScrollbar(actions);
    },
    after(actions) {
        deleteCookie.call(actions, 'lr');
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, ClarifyCategory.root);
                hideElementBySelector(actions, SearchResultList.root);
            },
        },
        ClarifyCategorySuite,
        SearchResults,
    ],
};
