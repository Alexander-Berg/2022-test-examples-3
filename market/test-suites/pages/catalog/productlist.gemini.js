import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import SearchResults from '@self/platform/spec/gemini/test-suites/blocks/catalog/snippet.gemini';
import AppPromoSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/AppPromo';

import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import SearchResultList from '@self/platform/spec/page-objects/SearchResults';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import VendorPromo from '@self/platform/spec/page-objects/VendorProductLinePromo';
import PremiumOffersGallery from '@self/platform/components/PremiumOffersGallery/__pageObject';
import SnippetPrice from '@self/project/src/components/SnippetPrice/__pageObject/SnippetPrice';

import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {hideElementBySelector, hideScrollbar} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const promoScrollBoxSelector = [
    // Любая из двух каруселей может появиться
    VendorPromo.root,
    PremiumOffersGallery.root,
].join(',');


export default {
    suiteName: 'ProductList',
    url: 'catalog--kholodilniki/71639/list',
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
                hideElementBySelector(actions, promoScrollBoxSelector);
            },
        },
        {
            ...SearchResults,
            ignore: [
                {every: SnippetPrice.root},
            ],
            before(actions) {
                // почему-то этот элемент приводит к сдвигу селекторов, поэтому его скрываем
                hideElementBySelector(actions, SearchOptions.root);
            },
        },
        {
            suiteName: 'PromoScrollBox',
            selector: promoScrollBoxSelector,
            ignore: [
                {every: VendorPromo.price},
            ],
            capture() {
            },
        },
        AppPromoSuite,
    ],
};
