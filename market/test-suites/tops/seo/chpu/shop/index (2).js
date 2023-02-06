import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createShopInfo, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createPrice} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

// suites
import ShopReviewsListChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopReviewsList/chpu';
import ShopCardChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ShopCard/chpu';
import ProductOffersChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffers/chpu';
// page-objects
import ShopReviewsList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';
import ShopCard from '@self/platform/spec/page-objects/widgets/content/ShopCard';
import ShopName from '@self/platform/spec/page-objects/components/ShopName';
import OfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';

import {productWithDefaultOffer, phoneProductRoute} from './kadavrProductMocks';

const SHOP_ID = 1925;
const SHOP_ID2 = 1926;
const SHOP_SLUG = 'tekhnopark';
const USER_UID = '100500';

const defaultReview = ({recommended}) => ({
    id: 1,
    shop: {
        id: SHOP_ID,
    },
    type: 0,
    cpa: recommended,
    pro: 'Lorem ipsum.',
    contra: 'Lorem ipsum.',
    comment: 'Lorem ipsum.',
    anonymous: 0,
    user: {
        uid: USER_UID,
    },
    photos: null,
});

export default makeSuite('Магазин', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Страница магазина', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        const review = defaultReview({recommended: true});
                        await this.browser.setState('report', createShopInfo({
                            entity: 'shop',
                            id: SHOP_ID,
                            status: 'actual',
                            oldStatus: 'actual',
                            slug: SHOP_SLUG,
                            ratingToShow: 3.166666667,
                            newGradesCount: 218,
                        }, SHOP_ID));

                        await this.browser.setState('schema', {
                            users: [createUser()],
                            modelOpinions: [review],
                        });

                        return this.browser.yaOpenPage('touch:shop', {shopId: SHOP_ID, slug: SHOP_SLUG});
                    },
                },
                prepareSuite(ShopReviewsListChpuSuite, {
                    pageObjects: {
                        shopReviewsList() {
                            return this.createPageObject(ShopReviewsList);
                        },
                    },
                    params: {
                        shopId: SHOP_ID,
                        slug: SHOP_SLUG,
                    },
                }),
                prepareSuite(ShopCardChpuSuite, {
                    pageObjects: {
                        shopCard() {
                            return this.createPageObject(ShopCard);
                        },
                    },
                    params: {
                        shopId: SHOP_ID,
                        slug: SHOP_SLUG,
                    },
                })
            ),
        }),
        makeSuite('Страница КМ', {
            story: prepareSuite(ProductOffersChpuSuite, {
                pageObjects: {
                    shopName() {
                        return this.createPageObject(ShopName);
                    },
                    offerSnippet() {
                        return this.createPageObject(OfferSnippet);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const offers = [];
                        const offersCount = 4;

                        for (let i = 0; i < offersCount; i++) {
                            offers.push(createOffer({
                                urls: {encrypted: 'https://ya.ru'},
                                price: createPrice(1000),
                                shop: {
                                    entity: 'shop',
                                    name: 'Технопарк',
                                    id: i % 2 ? SHOP_ID2 : SHOP_ID,
                                    status: 'actual',
                                    slug: SHOP_SLUG,
                                    overallGradesCount: 218,
                                },
                            }));
                        }

                        const state = mergeReportState([productWithDefaultOffer, ...offers, {
                            data: {
                                search: {
                                    total: offersCount,
                                    totalOffers: offersCount,
                                    totalOffersBeforeFilters: offersCount,
                                    totalModels: 0,
                                },
                            },
                        }]);

                        await this.browser.setState('report', state);
                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                    },
                },
                params: {
                    shopId: SHOP_ID,
                    slug: SHOP_SLUG,
                },
            }),
        })
    ),
});
