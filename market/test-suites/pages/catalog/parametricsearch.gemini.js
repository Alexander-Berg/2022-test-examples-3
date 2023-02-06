import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';

import SearchResultList from '@self/platform/spec/page-objects/SearchResults';
import SnippetPrice from '@self/project/src/components/SnippetPrice/__pageObject/SnippetPrice';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import SearchResult from '@self/platform/spec/page-objects/SearchResult';


import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {hideScrollbar, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'ParametricSearch',
    url: {
        pathname: '/catalog/54726/list',
        query: {
            hid: 91491,
            text: 'iphone 7 red 128 gb',
            glfilter: '13887626:13891866',
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
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, SearchResultList.root);
            },
        },
        {
            suiteName: 'SearchResultTile',
            selector: SearchResult.root,
            ignore: [
                {every: SnippetPrice.root},
            ],
            before(actions) {
                // почему-то этот элемент приводит к сдвигу селекторов, поэтому его скрываем
                hideElementBySelector(actions, SearchOptions.root);
            },
            capture() {},
        },
    ],
};
