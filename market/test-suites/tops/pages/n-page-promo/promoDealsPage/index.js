import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers/shop';
import {createOffer, mergeState, createShopCategories} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import ShopPromocodesSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoDealsHeader/_shop-promocodes';
import BreadcrumbsPromoDealsSuite from '@self/platform/spec/hermione/test-suites/blocks/Breadcrumbs/_promo-deals';
import FeedSuite from '@self/platform/spec/hermione/test-suites/blocks/Feed';
import FeedSnippetPromoDealsSuite from '@self/platform/spec/hermione/test-suites/blocks/FeedSnippet/_promo-deals';
// page-objects
import Feed from '@self/platform/spec/page-objects/Feed';
import FeedSnippet from '@self/platform/spec/page-objects/FeedSnippet';
import Breadcrumbs from '@self/platform/spec/page-objects/Journal/Breadcrumbs';
import PromoDealsHeader from '@self/platform/spec/page-objects/PromoDealsHeader';

import seoTemplate from './fixtures/seoTemplate';

const SHOP_ID = 10265670;
const SHOP_CATEGORIES_ID = 123488;
const SHOP_NAME = 'ShopName';
const SAMPLE_FEED = {
    id: '200346569',
    offerId: '16',
    categoryId: '34',
};

const SAMPLE_SHOP_CATEGORIES = {
    id: SHOP_CATEGORIES_ID,
    entity: 'shopCategories',
    categories: [
        {
            entity: 'category',
            id: 723087,
            name: 'Оборудование Wi-Fi и Bluetooth',
            slug: 'oborudovanie-wi-fi-i-bluetooth',
            fullName: 'Сетевое оборудование Wi-Fi и Bluetooth',
            type: 'guru',
        },
        {
            entity: 'category',
            id: 91105,
            name: 'TV-тюнеры',
            slug: 'tv-tiunery',
            fullName: 'TV-тюнеры',
        },
    ],
};

const PROMOCODE_PROMO = {
    type: 'promo-code',
    key: 'JvHToCHuy8D2wfLgxl4l7w',
    url: 'http://best.seller.ru/promos/500',
    startDate: '2018-01-31T21:00:00Z',
    promoCode: 'DATEDESCRIPTIONURL',
    discount: {
        value: 25,
    },
};

const SNIPPETS_COUNT_WITH_LOAD_MORE_BUTTON = 41;

export default makeSuite('Лендинг акций.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const offersState = [...new Array(SNIPPETS_COUNT_WITH_LOAD_MORE_BUTTON)].map((_, i) => createOffer({
                    promo: PROMOCODE_PROMO,
                    shop: {
                        feed: SAMPLE_FEED,
                        id: 1,
                        name: 'shop',
                        slug: 'shop',
                    },
                    navnodes: [{
                        entity: 'navnode',
                        fullName: 'Мобильные телефоны',
                        id: 54726,
                        isLeaf: true,
                        name: 'Мобильные телефоны',
                        rootNavnode: {},
                        slug: 'mobilnye-telefony',
                    }],
                    slug: 'offer',
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        offercard: '/redir/offercard',
                        geo: '/redir/geo',
                    },
                }, i));

                const shopState = createShopInfo({
                    'entity': 'shop',
                    'id': SHOP_ID,
                    'status': 'actual',
                    'oldStatus': 'actual',
                    'shopName': SHOP_NAME,
                    'slug': 'shop',
                }, SHOP_ID);

                const shopCategoriesState = createShopCategories(SAMPLE_SHOP_CATEGORIES, SHOP_CATEGORIES_ID);

                const reportState = mergeState([shopState, shopCategoriesState, ...offersState]);

                await this.browser.setState('Tarantino.data.result', [seoTemplate]);
                await this.browser.setState('report', reportState);

                return this.browser.yaOpenPage('market:promo', {
                    semanticId: 'promocodes_for_10265670',
                });
            },
        },

        prepareSuite(ShopPromocodesSuite, {
            params: {
                shopName: SHOP_NAME,
            },
            pageObjects: {
                promoDealsHeader() {
                    return this.createPageObject(PromoDealsHeader);
                },
            },
        }),

        prepareSuite(BreadcrumbsPromoDealsSuite, {
            pageObjects: {
                breadcrumbs() {
                    return this.createPageObject(Breadcrumbs);
                },
            },
        }),

        prepareSuite(FeedSuite, {
            params: {
                snippetsCountBeforeLoading: 40,
                snippetsCountAfterLoading: SNIPPETS_COUNT_WITH_LOAD_MORE_BUTTON,
            },
            pageObjects: {
                feed() {
                    return this.createPageObject(Feed);
                },
            },
        }),

        prepareSuite(FeedSnippetPromoDealsSuite, {
            pageObjects: {
                feedSnippet() {
                    return this.createPageObject(
                        FeedSnippet,
                        {
                            root: `${FeedSnippet.root}:nth-child(1)`,
                        }
                    );
                },
            },
        })
    ),
});
