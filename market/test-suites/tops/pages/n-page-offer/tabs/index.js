import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// suites
import DescriptionTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-summary/__description-title';
import SpecLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-summary/__spec-link';
import ProductSpecListSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-spec-list';
import SellerCommentTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-summary/__seller-comment-title';
import SellerCommentSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-card-purchase/__seller-comment';
import WarrantyTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-summary/__warranty-title';
import OfferCardReturnsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-card-returns';
import ProductTabsItemSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-tabs/__item';
import ProductTabsItemNameSimilarSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-tabs/__item_name_similar';
import OffercardTabsSuite from '@self/platform/spec/hermione/test-suites/blocks/offercard-tabs';
// page-objects
import SpecsFromFilters from '@self/platform/components/SpecsFromFilters/__pageObject';
import SellerComment from '@self/platform/widgets/content/OfferVisitCardSpecs/components/SellerComment/__pageObject';
import OfferVisitCardSpecs from '@self/platform/widgets/content/OfferVisitCardSpecs/__pageObject';
import Returns from '@self/platform/widgets/content/OfferVisitCardSpecs/components/Returns/__pageObject';
import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';

import {offer, offerId} from '../fixtures/offerWithoutModel';
import specsContent from './specs';
import similarContent from './similar';
import geoContent from './geo';
import reviewsContent from './reviews';
import config from './config';

export default makeSuite('Вкладки', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Контент вкладки "Описание"', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer', {offerId});
                    },
                },
                makeSuite('Информация об оффере', {
                    id: 'marketfront-3481',
                    issue: 'MARKETVERSTKA-34563',
                    story: {
                        'Блок "Описание".': mergeSuites(
                            {
                                beforeEach() {
                                    this.setPageObjects({
                                        offerSpecs: () => this.createPageObject(OfferVisitCardSpecs),
                                    });
                                },
                            },
                            prepareSuite(DescriptionTitleSuite),
                            prepareSuite(SpecLinkSuite),
                            prepareSuite(ProductSpecListSuite, {
                                pageObjects: {
                                    specList() {
                                        return this.createPageObject(SpecsFromFilters);
                                    },
                                },
                                only: ['По умолчанию'],
                            })
                        ),
                        'Блок "Комментарии магазина".': mergeSuites(
                            prepareSuite(SellerCommentTitleSuite, {
                                pageObjects: {
                                    sellerComment() {
                                        return this.createPageObject(SellerComment, {
                                            root: OfferVisitCardSpecs.root,
                                        });
                                    },
                                },
                            }),
                            prepareSuite(SellerCommentSuite, {
                                pageObjects: {
                                    offerCardPurchase() {
                                        return this.createPageObject(SellerComment, {
                                            root: OfferVisitCardSpecs.root,
                                        });
                                    },
                                },
                            })
                        ),
                    },
                }),

                makeSuite('Блок "Гарантии"', {
                    id: 'marketfront-3609',
                    issue: 'MARKETVERSTKA-35081',
                    story: mergeSuites(
                        prepareSuite(WarrantyTitleSuite, {
                            pageObjects: {
                                returns() {
                                    return this.createPageObject(Returns);
                                },
                            },
                        }),
                        prepareSuite(OfferCardReturnsSuite, {
                            pageObjects: {
                                offerCardReturns() {
                                    return this.createPageObject(Returns);
                                },
                            },
                        })
                    ),
                })

                /** MARKETFRONT-58886: Скип автотестов в релизе 2021.375.0
                makeSuite('Кнопка "В избранное"', {
                    story: prepareSuite(WishlistControlSuite, {
                        meta: {
                            id: 'marketfront-3608',
                            issue: 'MARKETVERSTKA-35080',
                        },
                        pageObjects: {
                            wishlistControl() {
                                return this.createPageObject(WishlistTumbler, {
                                    parent: FullCard.root,
                                });
                            },
                        },
                    }),
                })
                */
            ),
        }),

        mergeSuites(
            {
                async beforeEach() {
                    await this.setPageObjects({
                        productTabs: () => this.createPageObject(ProductTabs),
                    });
                    await this.browser.setState('report', offer);
                    await this.browser.yaOpenPage('market:offer', {offerId});
                },
            },
            createStories(
                config.routes,
                ({meta, params}) => prepareSuite(ProductTabsItemSuite, {
                    meta,
                    params,
                })
            )
        ),

        prepareSuite(ProductTabsItemNameSimilarSuite, {
            meta: {
                id: 'marketfront-3564',
                issue: 'MARKETVERSTKA-34928',
            },
            pageObjects: {
                productTabs() {
                    return this.createPageObject(ProductTabs);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', createOffer({
                        urls: {
                            encrypted: '/redir/encrypted',
                            decrypted: '/redir/decrypted',
                            offercard: '/redir/offercard',
                            geo: '/redir/geo',
                        },
                        shop: {
                            slug: 'shop',
                            name: 'shop',
                            id: 1,
                        },
                    }, offerId));
                    await this.browser.yaOpenPage('market:offer', {offerId, text: 'красный'});
                },
            },
        }),

        prepareSuite(OffercardTabsSuite, {
            hooks: {
                async beforeEach() {
                    const state = createOffer({
                        // для отображения вкладки «Карта»
                        shop: {
                            outletsCount: 3,
                        },
                        urls: {
                            encrypted: '/redir/encrypted',
                            decrypted: '/redir/decrypted',
                            geo: '/redir/geo',
                            offercard: '/redir/offercard',
                        },
                    }, offerId);

                    await this.browser.setState('report', state);

                    await this.browser.yaOpenPage('market:offer', {
                        offerId,
                    });
                },
            },
            pageObjects: {
                tabs() {
                    return this.createPageObject(ProductTabs);
                },
            },
            params: {
                expectedTabNames: [
                    'offer',
                    'specs',
                    'similar',
                    'reviews',
                ],
            },
        }),

        specsContent,
        similarContent,
        geoContent,
        reviewsContent
    ),
});
