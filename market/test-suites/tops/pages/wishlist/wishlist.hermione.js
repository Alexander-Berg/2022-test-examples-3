import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// helpers
import {mergeState, createProductForSku, createSku, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import WishlistZeroStateSuite from '@self/platform/spec/hermione/test-suites/blocks/Wishlist/zeroState';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import WishListDegradation from '@self/project/src/spec/hermione/test-suites/blocks/WishList/degradation';
import AnalogsSuite from '@self/project/src/spec/hermione/test-suites/blocks/WishList/analogs';

// page-objects
import ZeroState from '@self/project/src/widgets/content/Wishlist/__pageObject/components/ZeroState';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import WishList from '@self/platform/widgets/pages/WishlistPage/__pageObject';
import UnknownSnippet from '@self/project/src/components/Wishlist/Snippet/Unknown/__pageObject';
import SimilatPopup from '@self/root/src/widgets/content/Similar/__pageObject';

// mocks
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
import {createWishlistItem, createWishlistItemBySku, createWishlistState} from './fixtures/wishlist.mock';

const CASHBACK_AMOUNT = 100;
const CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: CASHBACK_AMOUNT,
};

const EXTRA_CASHBACK_PROMO = {
    type: 'blue-cashback',
    value: CASHBACK_AMOUNT,
    tags: ['extra-cashback'],
};

async function prepareAndSetState(isExtraCashback) {
    const OFFER = {
        ...cpaOfferMock,
        promos: isExtraCashback ? [EXTRA_CASHBACK_PROMO] : [CASHBACK_PROMO],
    };
    const offer = createOffer(OFFER, cpaOfferMock.wareId);
    await this.browser.setState('report', mergeState([
        {
            data: {
                search: {total: 1},
            },
        },
        offer,
    ]));
    const wishlistItem = createWishlistItem(cpaOfferMock.wareId);
    await this.browser.setState('persBasket', wishlistItem);
    await this.browser.yaLogin(
        profiles['pan-topinambur'].login,
        profiles['pan-topinambur'].password
    );
    await this.browser.yaOpenPage('touch:wishlist');
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница вишлиста', {
    // Will migrate to kadavr eventually
    environment: 'testing',
    feature: 'Страница вишлиста',
    story: mergeSuites(
        {
            beforeEach() {
                return this.browser.yaOpenPage('touch:wishlist');
            },

            'Рекламная подписка в начале страницы.': prepareSuite(WishlistZeroStateSuite, {
                pageObjects: {
                    zeroState() {
                        return this.createPageObject(ZeroState);
                    },
                },
            }),
        },
        {
            'Блок кешбэка': mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            cashbackDealTerms() {
                                return this.createPageObject(CashbackDealTerms);
                            },
                            cashbackInfoTooltip() {
                                return this.createPageObject(CashbackInfoTooltip);
                            },
                            cashbackDealText() {
                                return this.createPageObject(Text, {
                                    parent: this.cashbackDealTerms,
                                });
                            },
                        });
                    },
                },
                prepareSuite(CashbackDealTermSuite, {
                    meta: {
                        id: 'marketfront-4181',
                    },
                    hooks: {
                        async beforeEach() {
                            await prepareAndSetState.call(this, false);
                        },
                        afterEach() {
                            return this.browser.yaLogout();
                        },
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT,
                        cashbackFormat: 'full',
                        isTooltipOnHover: false,
                        isExtraCashback: false,
                    },
                }),
                prepareSuite(CashbackDealTermSuite, {
                    suiteName: 'Повышенный кешбэк',
                    meta: {
                        id: 'marketfront-4518',
                    },
                    hooks: {
                        async beforeEach() {
                            await prepareAndSetState.call(this, true);
                        },
                        afterEach() {
                            return this.browser.yaLogout();
                        },
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT,
                        cashbackFormat: 'full',
                        isTooltipOnHover: false,
                        isExtraCashback: true,
                    },
                })
            ),
        },
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

                    return this.browser.yaOpenPage('touch:wishlist');
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
        }),
        prepareSuite(WishListDegradation, {
            environment: 'kadavr',
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('touch:wishlist');
                },
            },
            pageObjects: {
                wishListPage() {
                    return this.createPageObject(WishList);
                },
            },
        })
    ),
});
