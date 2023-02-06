import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';

// suites
import OrderMinCostSuite from '@self/platform/spec/hermione/test-suites/blocks/OrderMinCost';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import GenericBundleDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import DeliveryBetterWithPlus
    from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/deliveryBetterWithPlus';
import ShopGoodsSuite from '@self/project/src/spec/hermione/test-suites/blocks/OfferShopGoods';
import ShopGoodsFromCategorySuite from '@self/project/src/spec/hermione/test-suites/blocks/OfferShopGoodsFromCategory';
import ShopReviewsSuite from '@self/project/src/spec/hermione/test-suites/blocks/ShopReviewsSuite';
import PromoFlashTermSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoFlash';
import SpreadDiscountCountSuite from '@self/project/src/spec/hermione/test-suites/blocks/promos/spreadDiscountCount';
import DirectDiscountPromo from '@self/root/src/spec/hermione/test-suites/blocks/DirectDiscountPromo';
import paymentSystemCashback from '@self/root/src/spec/hermione/test-suites/blocks/paymentSystemCashback';
import UnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc';
import CartPopupUnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/cartPopup';

// fixtures
import {cpaOffer, cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
import {prepareKadavrReportState} from '@self/project/src/spec/hermione/fixtures/promo/flash';
import {spreadDiscountCountPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';
import {createAndSetPaymentSystemCashbackOfferState} from '@self/root/src/spec/hermione/fixtures/offer/createAndSetPaymentSystemCashbackOfferState';
import {unitInfo} from '@self/platform/spec/hermione/fixtures/unitInfo';

// page-objects
import OrderMinCost from '@self/platform/spec/page-objects/components/OrderMinCost';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import OfferSummary from '@self/platform/widgets/parts/OfferSummary/__pageObject';
import DealsSticker from '@self/platform/spec/page-objects/DealsSticker';
import FreeDeliveryWithPlusLink from '@self/root/src/components/FreeDeliveryWithPlusLink/__pageObject';
import OfferSummaryShopName from '@self/platform/spec/page-objects/components/OfferSummaryShopName';
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import OfferPrice from '@self/platform/widgets/parts/OfferSummary/components/OfferPrice/__pageObject';
import PromoFlashDescription from '@self/project/src/components/BlueFlashDescription/__pageObject';
import TimerFlashSale from '@self/project/src/components/TimerFlashSale/__pageObject';
import DirectDiscountTerms from '@self/root/src/components/DirectDiscountTerms/__pageObject';
import UnitsCalc from '@self/root/src/components/UnitsCalc/__pageObject';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';

import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

import {
    expectedShopId,
    mainOfferId,
    mainOfferMock,
    navState,
    repState,
} from '@self/project/src/spec/hermione/fixtures/offer/offerMock';
import {UNITINFO_EXPECTED_TEXT} from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/constants';
import {getUnitInfoCollectionPath} from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/helpers';
import {
    prepareKadavrReportStateForDirectDiscount,
    promoText as directDiscountText,
} from '@self/project/src/spec/hermione/fixtures/promo/directDiscount';
import ageConfirmation from './ageConfirmation';
import automaticallyCalculatedDelivery from './automaticallyCalculatedDelivery';
import cutPrice from './cutPrice';
import complaint from './complaint';
import credit from './credit';
import russianPost from './russianPost';
import drugsDisclaimer from './drugsDisclaimer';
import offerDelivery from './delivery';
import cpc from './cpc';
import {bnplSuite} from './bnpl';

const CASHBACK_AMOUNT = 100;
const PAYMENT_SYSTEM_CASHBACK_AMOUNT = 200;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Карточка оффера', {
    environment: 'kadavr',
    story: {
        'Оффер.': {
            'Минимальная сумма заказа': mergeSuites(
                prepareSuite(OrderMinCostSuite, {
                    meta: {
                        id: 'm-touch-2399',
                        issue: 'MOBMARKET-9829',
                    },
                    pageObjects: {
                        orderMinCost() {
                            return this.createPageObject(OrderMinCost);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            const offer = createOffer({
                                shop: {
                                    id: 1,
                                    name: 'shop',
                                    slug: 'shop',
                                },
                                orderMinCost: {
                                    value: 5500,
                                    currency: 'RUR',
                                },
                            }, 1);
                            return this.browser.setState('report', offer)
                                .then(() => this.browser.yaOpenPage('touch:offer', {offerId: 1}));
                        },
                    },
                })
            ),
            'Кнопка "В корзину"': mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('Carter.items', []);
                        await this.browser.setState('report', cpaOffer);

                        return this.browser.yaOpenPage('touch:offer', {offerId: cpaOfferMock.wareId});
                    },
                },
                prepareSuite(CartButtonSuite, {
                    pageObjects: {
                        cartButton() {
                            return this.createPageObject(CartButton);
                        },
                    },
                }),
                prepareSuite(CartButtonCounterSuite, {
                    pageObjects: {
                        cartButton() {
                            return this.createPageObject(CartButton);
                        },
                    },
                }),
                prepareSuite(ItemCounterCartButtonSuite, {
                    meta: {
                        id: 'm-touch-3430',
                    },
                    params: {
                        counterStep: cpaOfferMock.bundleSettings.quantityLimit.step,
                        offerId: cpaOfferMock.wareId,
                    },
                    pageObjects: {
                        parent() {
                            return this.createPageObject(OfferSummary);
                        },
                        cartButton() {
                            return this.createPageObject(CartButton, {
                                parent: OfferSummary.root,
                            });
                        },
                        counterCartButton() {
                            return this.createPageObject(CounterCartButton, {
                                parent: OfferSummary.root,
                            });
                        },
                    },
                })
            ),

            'Промо.': prepareSuite(SpreadDiscountCountSuite, {
                pageObjects: {
                    cartButton() {
                        return this.createPageObject(CartButton, {
                            parent: OfferSummary.root,
                        });
                    },
                    counterCartButton() {
                        return this.createPageObject(CounterCartButton, {
                            parent: OfferSummary.root,
                        });
                    },
                    price() {
                        return this.createPageObject(OfferPrice, {
                            parent: OfferSummary.root,
                        });
                    },
                },
                hooks: {
                    async beforeEach() {
                        const offer = createOffer({
                            ...cpaOfferMock,
                            promos: [spreadDiscountCountPromo],
                        }, cpaOfferMock.wareId);
                        await this.browser.setState('Carter.items', []);
                        await this.browser.setState('report', offer);

                        await this.browser.yaOpenPage('touch:offer', {offerId: cpaOfferMock.wareId});

                        return this.cartButton.click();
                    },
                },
                params: {
                    promoBound: spreadDiscountCountPromo.itemsInfo.bounds[0],
                },
            }),

            'Блок с информацией о магазине.': prepareSuite(ShopReviewsSuite,
                {
                    pageObjects: {
                        shopInfo() {
                            return this.createPageObject(OfferSummaryShopName);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', repState);
                            await this.browser.yaOpenPage('touch:offer', {offerId: mainOfferId});
                        },
                    },
                    params: {
                        shopId: mainOfferMock.shop.id,
                        slug: mainOfferMock.shop.slug,
                    },
                }
            ),
            'Блок акционного кешбэка по платежной системе.': mergeSuites(
                prepareSuite(paymentSystemCashback({shouldShow: false}), {
                    suiteName: 'Незалогин',
                    meta: {
                        id: 'marketfront-5242',
                    },
                    params: {
                        isAuthWithPlugin: false,
                        prepareState: async function () {
                            await createAndSetPaymentSystemCashbackOfferState.call(this, {
                                isPromoAvailable: false,
                                isPromoProduct: false,
                                hasCashback: true,
                                page: 'touch:offer',
                            });
                        },
                    },
                }),
                prepareSuite(paymentSystemCashback({shouldShow: false}), {
                    suiteName: 'Акция недоступна для пользователя',
                    meta: {
                        id: 'marketfront-5240',
                    },
                    params: {
                        prepareState: async function () {
                            await createAndSetPaymentSystemCashbackOfferState.call(this, {
                                isPromoAvailable: false,
                                isPromoProduct: false,
                                hasCashback: true,
                                page: 'touch:offer',
                            });
                        },
                    },
                }),
                prepareSuite(paymentSystemCashback({shouldShow: false}), {
                    suiteName: 'Выбран не акционный товар',
                    meta: {
                        id: 'marketfront-5241',
                    },
                    params: {
                        prepareState: async function () {
                            await createAndSetPaymentSystemCashbackOfferState.call(this, {
                                isPromoAvailable: true,
                                isPromoProduct: false,
                                hasCashback: true,
                                page: 'touch:offer',
                            });
                        },
                    },
                }),
                prepareSuite(paymentSystemCashback({shouldShow: true}), {
                    suiteName: 'Акционный кешбэк отображается вместе с обычным кешбэком',
                    meta: {
                        id: 'marketfront-5238',
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT,
                        paymentSystemCashbackAmount: PAYMENT_SYSTEM_CASHBACK_AMOUNT,
                        prepareState: async function () {
                            await createAndSetPaymentSystemCashbackOfferState.call(this, {
                                isPromoAvailable: true,
                                isPromoProduct: true,
                                hasCashback: true,
                                page: 'touch:offer',
                            });
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETFRONT-63723');
                        },
                    },
                }),
                prepareSuite(paymentSystemCashback({shouldShow: true}), {
                    suiteName: 'Отображается только акционный кешбэк',
                    meta: {
                        id: 'marketfront-5239',
                    },
                    params: {
                        paymentSystemCashbackAmount: PAYMENT_SYSTEM_CASHBACK_AMOUNT,
                        prepareState: async function () {
                            await createAndSetPaymentSystemCashbackOfferState.call(this, {
                                isPromoAvailable: true,
                                isPromoProduct: true,
                                page: 'touch:offer',
                            });
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETFRONT-63723');
                        },
                    },
                })
            ),
            'Блок кешбэка.': mergeSuites(
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
                        id: 'marketfront-4177',
                    },
                    hooks: {
                        async beforeEach() {
                            const localOfferId = 'uQizLmsYjkLixn5SRhgitQ';
                            const localOffer = createOffer({
                                shop: {
                                    id: 1,
                                    name: 'shop',
                                    slug: 'shop',
                                    outletsCount: 1,
                                },
                                urls: {
                                    offercard: '/redir/offercard',
                                    geo: '/redir/geo',
                                },
                                promos: [{
                                    type: 'blue-cashback',
                                    value: CASHBACK_AMOUNT,
                                }],
                            }, localOfferId);
                            await this.browser.setState('report', localOffer);

                            await this.browser.yaOpenPage('touch:offer', {offerId: localOfferId});
                        },
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT,
                        cashbackFormat: 'full',
                        isTooltipOnHover: true,
                        isExtraCashback: false,
                    },
                }),
                prepareSuite(CashbackDealTermSuite, {
                    suiteName: 'Повышенный кешбэк.',
                    meta: {
                        id: 'marketfront-4498',
                    },
                    hooks: {
                        async beforeEach() {
                            const localOfferId = 'uQizLmsYjkLixn5SRhgitQ';
                            const localOffer = createOffer({
                                shop: {
                                    id: 1,
                                    name: 'shop',
                                    slug: 'shop',
                                    outletsCount: 1,
                                },
                                urls: {
                                    offercard: '/redir/offercard',
                                    geo: '/redir/geo',
                                },
                                promos: [{
                                    type: 'blue-cashback',
                                    value: CASHBACK_AMOUNT,
                                    tags: ['extra-cashback'],
                                }],
                            }, localOfferId);
                            await this.browser.setState('report', localOffer);

                            await this.browser.yaOpenPage('touch:offer', {offerId: localOfferId});
                        },
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT,
                        cashbackFormat: 'full',
                        isTooltipOnHover: true,
                        isExtraCashback: true,
                    },
                })
            ),
            'Блок с подарком': mergeSuites(
                prepareSuite(GenericBundleDealTermSuite, {
                    params: {
                        withPromoBlock: true,
                    },
                    pageObjects: {
                        dealsSticker() {
                            return this.createPageObject(DealsSticker);
                        },
                    },
                    meta: {
                        id: 'marketfront-4264',
                    },
                    hooks: {
                        async beforeEach() {
                            const {
                                stateWithProductOffers,
                                primary,
                            } = prepareKadavrReportStateWithDefaultState();

                            await this.browser.setState('report', stateWithProductOffers);
                            await this.browser.setState('Carter.items', []);
                            await this.browser.yaOpenPage('touch:offer', {offerId: primary.offerMock.offerId});
                        },
                    },
                })
            ),
            'Флэш акция': mergeSuites(
                prepareSuite(PromoFlashTermSuite, {
                    pageObjects: {
                        timerFlashSale() {
                            return this.createPageObject(TimerFlashSale);
                        },
                        promoFlashDescription() {
                            return this.createPageObject(PromoFlashDescription);
                        },
                    },
                    meta: {
                        id: 'marketfront-4306',
                    },
                    hooks: {
                        async beforeEach() {
                            const {
                                state: flashState,
                                offerId: wareId,
                            } = prepareKadavrReportState();

                            await this.browser.setState('report', flashState);
                            await this.browser.setState('Carter.items', []);
                            await this.browser.yaOpenPage('touch:offer', {offerId: wareId});
                        },
                    },
                })
            ),
            'Прямая скидка': mergeSuites(
                prepareSuite(DirectDiscountPromo, {
                    pageObjects: {
                        directDiscountTerms() {
                            return this.createPageObject(DirectDiscountTerms);
                        },
                    },
                    meta: {
                        id: 'marketfront-4345',
                    },
                    params: {
                        promoText: directDiscountText,
                    },
                    hooks: {
                        async beforeEach() {
                            const {
                                state,
                                offerId: wareId,
                            } = prepareKadavrReportStateForDirectDiscount();

                            await this.browser.setState('report', state);
                            await this.browser.setState('Carter.items', []);
                            await this.browser.yaOpenPage('touch:offer', {offerId: wareId});
                        },
                    },
                })
            ),

            'Блок "Товары от магазина"': prepareSuite(ShopGoodsSuite, {
                pageObjects: {
                    scrollBox() {
                        return this.createPageObject(ScrollBox, {
                            parent: '[data-zone-data*="Товары от магазина"]',
                        });
                    },
                    shopInfo() {
                        return this.createPageObject(OfferSummaryShopName);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', repState);
                        await this.browser.yaOpenPage('touch:offer', {offerId: mainOfferId});
                    },
                },
                params: {
                    expectedShopId,
                },
                meta: {
                    id: 'marketfront-4761',
                    issue: 'MARKETFRONT-45053',
                },
            }),

            'Блок "Товары из категории от магазина"': prepareSuite(ShopGoodsFromCategorySuite, {
                pageObjects: {
                    scrollBox() {
                        return this.createPageObject(ScrollBox, {
                            parent: '[data-zone-data*="Товары из категории от магазина"]',
                        });
                    },
                    shopInfo() {
                        return this.createPageObject(OfferSummaryShopName);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', repState);
                        await this.browser.setState('Catalog.tree', navState);
                        await this.browser.yaOpenPage('touch:offer', {offerId: mainOfferId});
                    },
                },
                params: {
                    expectedShopId,
                },
                meta: {
                    id: 'marketfront-4763',
                    issue: 'MARKETFRONT-45056',
                },
            }),

            'Выгода Плюса.': prepareSuite(DeliveryBetterWithPlus, {
                params: {
                    pageId: 'touch:offer',
                },
                pageObjects: {
                    freeDeliveryWithPlusLink() {
                        return this.createPageObject(FreeDeliveryWithPlusLink);
                    },
                },
            }),

            'Калькулятор упаковок.': prepareSuite(UnitsCalcSuite, {
                pageObjects: {
                    unitsCalc() {
                        return this.createPageObject(UnitsCalc, {
                            parent: OfferSummary.root,
                        });
                    },
                },
                params: {
                    expectedText: UNITINFO_EXPECTED_TEXT,
                },
                meta: {
                    id: 'marketfront-5766',
                    issue: 'MARKETFRONT-79800',
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', cpaOffer);
                        await this.browser.setState(
                            getUnitInfoCollectionPath(cpaOfferMock.wareId),
                            unitInfo
                        );
                        await this.browser.setState(
                            `report.collections.offer.${cpaOfferMock.wareId}.navnodes`,
                            [{
                                ...cpaOfferMock.navnodes[0],
                                tags: ['unit_calc'],
                            }]
                        );
                        await this.browser.yaOpenPage('touch:offer', {offerId: cpaOfferMock.wareId});
                    },
                },
            }),

            'Калькулятор упаковок в попапе.': prepareSuite(CartPopupUnitsCalcSuite, {
                hooks: {
                    async beforeEach() {
                        this.setPageObjects({
                            cartButton: () => this.createPageObject(CartButton, {
                                parent: OfferSummary.root,
                            }),
                            cartPopup: () => this.createPageObject(CartPopup),
                            counterCartButton: () => this.createPageObject(CounterCartButton, {
                                parent: this.cartPopup,
                            }),
                            unitsCalc: () => this.createPageObject(UnitsCalc, {
                                parent: this.cartPopup,
                            }),
                        });

                        await this.browser.setState('report', cpaOffer);
                        await this.browser.setState(
                            getUnitInfoCollectionPath(cpaOfferMock.wareId),
                            unitInfo
                        );
                        await this.browser.setState(
                            `report.collections.offer.${cpaOfferMock.wareId}.navnodes`,
                            [{
                                ...cpaOfferMock.navnodes[0],
                                tags: ['unit_calc'],
                            }]
                        );
                        await this.browser.yaOpenPage('touch:offer', {offerId: cpaOfferMock.wareId});

                        await this.cartButton.click();

                        await this.browser.waitForVisible(CartPopup.root, 10000);
                    },
                },
                pageObjects: {
                    unitsCalc() {
                        return this.createPageObject(UnitsCalc, {
                            parent: OfferSummary.root,
                        });
                    },
                    productDefaultOffer() {
                        return this.createPageObject(OfferSummary);
                    },
                },
            }),

            ageConfirmation,
            automaticallyCalculatedDelivery,
            cutPrice,
            russianPost,
            credit,
            'Лекарственный дисклеймер.': drugsDisclaimer,
            'Жалоба.': complaint,
            'Доставка в ДО.': offerDelivery,
            cpc,
            bnplSuite,
        },
    },
});
