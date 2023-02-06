import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {
    createSku,
    createOffer,
    mergeState,
    createProductForSku,
} from '@yandex-market/kadavr/mocks/Report/helpers';

// test-suites
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import WelcomeCashbackSuite from '@self/platform/spec/hermione/test-suites/blocks/WelcomeCashbackSuite/wishlist';
import AnalogsSuite from '@self/project/src/spec/hermione/test-suites/blocks/WishList/analogs';
import WishListDegradation from '@self/project/src/spec/hermione/test-suites/blocks/WishList/degradation.js';

// page-objects
import FooterMarket from '@self/platform/spec/page-objects/footer-market';
import SnippetOfferCell from '@self/project/src/components/Search/Snippet/Offer/Cell/__pageObject';
import WishList from '@self/platform/widgets/content/Wishlist/__pageObject';
import UnknownSnippet from '@self/project/src/widgets/content/Wishlist/components/UnknownSnippet/__pageObject';
import SimilatPopup from '@self/root/src/widgets/content/Similar/__pageObject';

// fixtures
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';

import {createWishlistItem, createWishlistItemBySku, createWishlistState} from './fixtures/wishlist.mock';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вишлиста.', {
    issue: 'MARKETVERSTKA-25711',
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    footerMarket() {
                        return this.createPageObject(FooterMarket);
                    },
                });
                return this.browser.yaOpenPage('market:wishlist');
            },
        },
        makeSuite('Выдача.', {
            story: {
                'Гридовый список сниппетов, DSBS-оффер': mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                snippet() {
                                    return this.createPageObject(SnippetOfferCell, {
                                        root: `${SnippetOfferCell.root}:nth-child(1)`,
                                    });
                                },
                            });

                            const state = mergeState([
                                createOffer(offerDSBSMock, offerDSBSMock.id),
                                {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                },
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);
                            await this.browser.setState('persBasket', {
                                ...createWishlistItem(offerDSBSMock.id),
                                hasMore: false,
                                token: 'token',
                            });

                            return this.browser.yaOpenPage('market:wishlist', {
                                track: 'menu',
                            });
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonSuite)
                ),
            },
        }),
        prepareSuite(WelcomeCashbackSuite),
        prepareSuite(WishListDegradation, {
            environment: 'kadavr',
            pageObjects: {
                wishListPage() {
                    return this.createPageObject(WishList);
                },
            },
        }),
        prepareSuite(AnalogsSuite, {
            hooks: {
                async beforeEach() {
                    const outOfStockWishlistItem = createWishlistItemBySku({
                        id: sock.skuMock.id,
                        references: {hid: '14870981', productId: sock.productMock.id},
                    });

                    const reportState = mergeState([
                        createProductForSku(sock.productMock, sock.skuMock.id, sock.productMock.id),
                        createSku(sock.skuMock, sock.skuMock.id),
                        {
                            data: {
                                search: {
                                    results: [
                                        {schema: 'product', id: sock.productMock.id},
                                        {schema: 'sku', id: sock.skuMock.id},
                                    ],
                                    totalOffers: 1,
                                    total: 1,
                                },
                            },
                        },
                    ]);

                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', reportState);
                    await this.browser.setState('persBasket', createWishlistState([
                        outOfStockWishlistItem,
                    ]));

                    return this.browser.yaOpenPage('market:wishlist');
                },
            },
            pageObjects: {
                unknownSnippet() {
                    return this.createPageObject(UnknownSnippet);
                },

                similarPopup() {
                    return this.createPageObject(SimilatPopup);
                },
            },
        })
    ),
});
