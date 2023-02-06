import {setCookies} from '@yandex-market/gemini-extended-actions';
import cookies from '@self/platform/constants/cookie';

import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer/';
import DefaultOfferPrice from '@self/platform/spec/page-objects/components/DefaultOffer/DefaultOfferPrice';
import ProductOffersStaticList from '@self/platform/widgets/parts/ProductOffersStaticList/__pageObject';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import Review from '@self/platform/spec/page-objects/ProductReview';
import Reviews from '@self/platform/spec/page-objects/widgets/parts/ProductReviews';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import ProductOffersSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import {
    hideRegionPopup,
    hideModalFloat,
    hideElementBySelector,
    hideHeader,
    hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KMModelReviews',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.AGGRESSIVE_SMART_BANNER_HIDDEN,
                value: '1',
            },
            {
                name: cookies.SIMPLE_SMART_BANNER_HIDDEN,
                value: '1',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideHeader(actions);
        hideHeadBanner(actions);
    },
    childSuites: [
        // Для быстрой инициализации ленивых виджетов, в скринтестах где не скриншутятся отзывы сначала вырезаем отзывы.
        {
            ...MainSuite,
            url: '/product--smartfon-samsung-galaxy-s8/1722193751/reviews',
            before(actions) {
                hideElementBySelector(actions, Reviews.root);
                initLazyWidgets(actions, 2000);
                hideElementBySelector(actions, ProductOffersStaticList.root);
                hideElementBySelector(actions, '[data-zone-data*="journal"]');
                actions.wait(500); // боремся с тряской
            },
        },
        {
            ...MainSuite,
            suiteName: 'ReviewsPageWithUgcVideo',
            url: '/product--kompiuternaia-garnitura-sven-ap-g888mv/13859021/reviews',
            before(actions) {
                initLazyWidgets(actions, 2000);
                hideElementBySelector(actions, ProductOffersStaticList.root);
                hideElementBySelector(actions, '[data-zone-data*="journal"]');
                hideElementBySelector(actions, Reviews.root);
                actions.wait(500); // боремся с тряской
            },
        },
        {
            suiteName: 'firstReviewId-GoodGuyT.',
            selector: Review.root,
            url: '/product/150334300/reviews?firstReviewId=93298550',
            capture() {},
        },
        {
            suiteName: 'DefaultOffer',
            url: '/product--igrovaia-pristavka-sony-playstation-5-digital-edition-825-gb/665468003/reviews',
            selector: DefaultOffer.root,
            ignore: [
                DefaultOfferPrice.root,
                ShopRating.root,
            ],
            before(actions) {
                hideElementBySelector(actions, Reviews.root);
                initLazyWidgets(actions, 2000);
                actions.wait(500); // боремся с тряской
            },
            capture() {},
        },
        {
            suiteName: 'OfferSnippet',
            url: '/product--igrovaia-pristavka-sony-playstation-5-digital-edition-825-gb/665468003/reviews',
            selector: ProductOffersSnippet.root,
            ignore: [
                {every: '[data-zone-name="reviews-count"]'},
                '[data-additional-zone="mainPrice"]',
            ],
            before(actions) {
                initLazyWidgets(actions, 2000);
                hideElementBySelector(actions, '[data-zone-name="productCardReviewsRating"]');
                hideElementBySelector(actions, Reviews.root);
                actions.wait(500); // боремся с тряской
            },
            capture() {},
        },
    ],
};
