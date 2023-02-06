import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import SearchResults from '@self/platform/spec/page-objects/SearchResults';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import SearchResultWrapper from '@self/platform/containers/SearchResult/Wrapper/__pageObject';

import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'ReviewsHub',
    url: '/catalog--kholodilniki/71639/list?hid=15450081&show-reviews=1',
    before(actions) {
        setDefaultGeminiCookies(actions);
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');

        new ClientAction(actions).removeElems(selector);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                const selector = [
                    SearchResults.root,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        {
            suiteName: 'Review',
            selector: `${SearchResultWrapper.root}:nth-child(3)`,
            before(actions) {
                // почему-то этот элемент приводит к сдвигу селекторов, поэтому его скрываем
                hideElementBySelector(actions, SearchOptions.root);
            },
            capture(actions) {
                actions.waitForElementToShow(`${SearchResultWrapper.root}:nth-child(3)`, 1000);
            },
        },
    ],
};
