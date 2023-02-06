import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import SearchResults from '@self/platform/spec/gemini/test-suites/blocks/catalog/offers-snippet.gemini';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {hideScrollbar, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import SearchResultList from '@self/platform/spec/page-objects/SearchResults';
import VendorPromo from '@self/platform/spec/page-objects/VendorProductLinePromo';


export default {
    suiteName: 'OffersList',
    url: '/catalog/57647/list?hid=90527',
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
                hideElementBySelector(actions, VendorPromo.root);
            },
        },
        SearchResults,
    ],
};
