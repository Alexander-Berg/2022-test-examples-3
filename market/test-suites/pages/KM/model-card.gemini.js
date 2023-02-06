import {setCookies} from '@yandex-market/gemini-extended-actions';
import cookies from '@self/platform/constants/cookie';

import Offers from '@self/platform/spec/gemini/test-suites/blocks/KM/offers.gemini';
import AveragePrice from '@self/platform/spec/gemini/test-suites/blocks/KM/average-price.gemini';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import PopularQuestions from '@self/platform/spec/gemini/test-suites/blocks/KM/popular-questions.gemini';

import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import DefaultOfferPrice from '@self/platform/spec/page-objects/components/DefaultOffer/DefaultOfferPrice';
import OfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import VendorPromoProductGroup from '@self/platform/spec/page-objects/widgets/content/VendorPromoProductGroup';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import ProductOffers from '@self/platform/spec/page-objects/widgets/parts/ProductOffers';
import InterestBadge from '@self/platform/widgets/parts/ProductCard/ProductCardInterestBadge/__pageObject';

import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideAveragePrice,
    hideOffers,
    hidePopularQuestions,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';


export default {
    suiteName: 'KM',
    url: '/product--igrovaia-pristavka-sony-playstation-5-digital-edition-825-gb/665468003',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.VENDOR_PROMO_BADGE_ONBOARDING_DISABLED,
                value: '1',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        disableAnimations(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                VendorPromoProductGroup.root,
                {every: Counter.root},
                // счетчики на ссылках на табы
                '[data-autotest-id="reviews"] span',
                '[data-zone-name="questions"] span > span',
                InterestBadge.root,
            ],
            before(actions) {
                initLazyWidgets(actions, 5000);
                disableAnimations(actions);
                hideAveragePrice(actions);
                hideOffers(actions);
                hidePopularQuestions(actions);
            },
        },
        {
            ...Offers,
            ignore: [
                ProductOffers.counter,
                '[id="scroll-to-productOffers"] > div',
                {every: DefaultOfferPrice.root},
                {every: OfferSnippet.price},
                {every: '[data-zone-name="reviews-count"]'},
            ],
            before(actions) {
                // скрываем только ДО
                hideElementBySelector(actions, DefaultOffer.root);
            },
        },
        {
            suiteName: 'DefaultOffer',
            selector: `${DefaultOffer.root} > div > div`,
            ignore: [DefaultOfferPrice.root, '[data-zone-name="reviews-count"]', ShopRating.root],
            capture() {},
        },
        AveragePrice,
        {
            ...PopularQuestions,
            ignore: '[data-zone-name="answers"]',
        },
    ],
};
