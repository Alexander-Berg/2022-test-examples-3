import {setCookies} from '@yandex-market/gemini-extended-actions';
import AveragePrice from '@self/platform/spec/gemini/test-suites/blocks/KM/average-price.gemini';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Offers from '@self/platform/spec/gemini/test-suites/blocks/KM/offers.gemini';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import DefaultOfferPrice from '@self/platform/spec/page-objects/components/DefaultOffer/DefaultOfferPrice';
import ProductOffers from '@self/platform/spec/page-objects/widgets/parts/ProductOffers';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';


import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideAveragePrice,
    hideOffers,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'AlcoholCard',
    url: 'product--koniak-camus-elegance-vsop-0-5-l-podarochnaia-upakovka/419331081',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'adult',
                value: '1:1:ADULT',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A11%3A53.081539.png
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 5000);
                hideAveragePrice(actions);
                hideOffers(actions);
                actions.wait(500); // боремся с тряской
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A07%3A14.603624.jpg
            ...Offers,
            ignore: [
                ProductOffers.counter,
                {every: DefaultOfferPrice.root},
                {every: '[data-autotest-id="offer-price"]'},
                {every: '[data-zone-name="reviews-count"]'},
            ],
            before(actions) {
                // скрываем только ДО
                hideElementBySelector(actions, DefaultOffer.root);
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A06%3A50.712429.jpg
            suiteName: 'DefaultOffer',
            selector: DefaultOffer.root,
            ignore: [DefaultOfferPrice.root, '[data-zone-name="reviews-count"]', ShopRating.root],
            capture() {},
        },
        AveragePrice,
    ],
};
