import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import DefaultOfferPrice from '@self/platform/spec/page-objects/components/DefaultOffer/DefaultOfferPrice';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import Mooa from '@self/platform/spec/page-objects/mooa';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ProductPage from '@self/platform/spec/page-objects/ProductPage';
import ProductOffersStaticList from '@self/platform/widgets/parts/ProductOffersStaticList/__pageObject';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import ComplainSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/compain.gemini';
import ProductComplainButton from '@self/platform/spec/page-objects/components/ProductComplaintButton';
import StickyOffer from '@self/platform/widgets/parts/StickyOffer/__pageObject';

import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {hideScrollbox, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'KMSpec',
    url: '/product--igrovaia-pristavka-sony-playstation-5-digital-edition-825-gb/665468003/spec',
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
            suiteName: 'Main',
            ignore: {every: Counter.root},
            selector: ProductPage.root,
            before(actions) {
                initLazyWidgets(actions);
                hideElementBySelector(actions, ProductOffersStaticList.root);
                hideScrollbox(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(ProductPage.root, 10000);
            },
        },
        {
            suiteName: 'DefaultOffer',
            selector: `${ProductOffersStaticList.root} > div > div`,
            ignore: [
                DefaultOfferPrice.root,
                ShopRating.root,
            ],
            // scroll near widget and wait it for show
            before(actions) {
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(
                    'document.querySelector(\'[data-autotest-id="reviews"]\').scrollIntoView()'));
                actions.waitForElementToShow(DefaultOfferPrice.root);
            },
            capture() {},
        },
        {
            suiteName: 'OfferSnippet',
            selector: `${ProductOffersStaticList.root} [data-zone-name="offerSnippet"]`,
            ignore: [
                {every: '[data-zone-name="reviews-count"]'},
                '[data-additional-zone="mainPrice"]',

            ],
            // scroll near widget and wait it for show
            before(actions) {
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(
                    'document.querySelector(\'[data-autotest-id="reviews"]\').scrollIntoView()'));
                actions.waitForElementToShow(`${ProductOffersStaticList.root} [data-zone-name="offerSnippet"]`);
            },
            capture() {},
        },
        {
            suiteName: 'ScrollBox',
            selector: ScrollBox.root,
            // На мобильных телефонах, на плойке - нет статей. На пиле есть.
            url: '/product--elektricheskaia-pila-makita-uc3541a-1800-vt/12878273/spec',
            ignore: [
                {every: 'picture'},
                {every: Counter.root},
            ],
            before(actions) {
                initLazyWidgets(actions, 5000);
            },
            capture() {},
        },
        {
            ...ComplainSuite,
            before(actions, find) {
                hideElementBySelector(actions, StickyOffer.root);
                actions.wait(50);
                actions.click(find(ProductComplainButton.root));
                // надо дать попапу прогрузиться
                actions.wait(1000);
            },
        },
    ],
};
