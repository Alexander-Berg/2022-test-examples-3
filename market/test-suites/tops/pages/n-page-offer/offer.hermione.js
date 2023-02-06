import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {createOffer, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import ProductOffersSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/product-offers';
import OfferSummarySuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-summary';
import SearchSimilarReportTextParamSuite from '@self/platform/spec/hermione/test-suites/blocks/n-search-similar/report-text-param';
import GeoTextParamSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo/report-text-param';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import GenericBundleDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import PromoFlashTermSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoFlash';
import DSBSOffersSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/dsbs-offers';
import DeliveryBetterWithPlus from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/deliveryBetterWithPlus';
import ShopGoodsSuite from '@self/project/src/spec/hermione/test-suites/blocks/OfferShopGoods';
import ShopGoodsFromCategorySuite from '@self/project/src/spec/hermione/test-suites/blocks/OfferShopGoodsFromCategory';
import offerDegradationSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-degradation';
import DirectDiscountPromo from '@self/root/src/spec/hermione/test-suites/blocks/DirectDiscountPromo';
import paymentSystemCashback from '@self/root/src/spec/hermione/test-suites/blocks/paymentSystemCashback';
import UnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc';
import CartPopupUnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/cartPopup';
import PharmaDefaultOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/pharma/defaultOffer';

// page-objects
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import ShopName from '@self/project/src/components/ShopName/__pageObject';
import ShopInfo from '@self/project/src/components/ShopInfo/__pageObject';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';
import ShopInfoDrugsDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/drugsDisclaimer';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import DealsSticker from '@self/platform/spec/page-objects/DealsSticker';
import FreeDeliveryWithPlusLink from '@self/root/src/components/FreeDeliveryWithPlusLink/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import PromoFlashDescription from '@self/project/src/components/BlueFlashDescription/__pageObject';
import TimerFlashSale from '@self/project/src/components/TimerFlashSale/__pageObject';
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import UnitsCalc from '@self/root/src/components/UnitsCalc/__pageObject';
import DirectDiscountTerms from '@self/root/src/components/DirectDiscountTerms/__pageObject';
import AmountSelect from '@self/project/src/components/AmountSelect/__pageObject';
import Price from '@self/platform/components/Price/__pageObject';

// fixtures
import {cpaOffer, cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import {prepareKadavrReportState} from '@self/project/src/spec/hermione/fixtures/promo/flash';
import {promoText as directDiscountText, prepareKadavrReportStateForDirectDiscount} from '@self/project/src/spec/hermione/fixtures/promo/directDiscount';
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';
import {createAndSetPaymentSystemCashbackOfferState} from '@self/root/src/spec/hermione/fixtures/offer/createAndSetPaymentSystemCashbackOfferState';
import {unitInfo} from '@self/platform/spec/hermione/fixtures/unitInfo';

// imports
import {UNITINFO_EXPECTED_TEXT} from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/constants';
import {getUnitInfoCollectionPath} from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/helpers';
import {mainOfferId, repState, navState, expectedShopId} from '@self/project/src/spec/hermione/fixtures/offer/offerMock';
import {offer, offerId} from './fixtures/offerWithoutModel';
import disclaimers from './disclaimers';
import offerWithoutModelSimilar from './offerWithoutModelSimilar';
import popupComplain from './popup-complain';
import ageConfirmation from './ageConfirmation';
import metrika from './metrika';
import shopsInfo from './offerWithoutModelShopsInfo';
import tabs from './tabs';
import {bnplSuite} from './bnpl';
import credit from './credit';
import unitInfoDefaultOfferSuite from './unitInfo/defaultOffer';


const TEXT_PARAM = 'красный';
const CASHBACK_AMOUNT = 100;
const PAYMENT_SYSTEM_CASHBACK_AMOUNT = 200;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница оффера.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Блок "Информация о магазине".', {
            feature: 'Информация о продавце',
            story: mergeSuites(
                prepareSuite(ProductOffersSuite, {
                    pageObjects: {
                        shopName() {
                            return this.createPageObject(ShopName);
                        },
                        shopsInfo() {
                            return this.createPageObject(LegalInfo);
                        },
                    },
                    params: {
                        shopIds: [2],
                    },
                    meta: {
                        id: 'marketfront-2202',
                        issue: 'MARKETVERSTKA-27297',
                    },
                    hooks: {
                        beforeEach() {
                            const parentProductId = 1;
                            const offerWithModel = createOffer({
                                model: {
                                    id: parentProductId,
                                },
                                shop: {
                                    entity: 'shop',
                                    slug: 'shop',
                                    name: 'shop',
                                    id: 2,
                                },
                                urls: {
                                    encrypted: '/redir/encrypted',
                                    decrypted: '/redir/decrypted',
                                    offercard: '/redir/offercard',
                                    geo: '/redir/geo',
                                },
                                seller: {
                                    comment: 'Comment',
                                },
                            }, offerId);
                            const product = createProduct({
                                slug: 'product',
                            }, parentProductId);

                            return this.browser.setState('report', mergeState([
                                offerWithModel,
                                product,
                                {
                                    data: {
                                        search: {
                                            total: 1,
                                        },
                                    },
                                },
                            ])).then(() => this.browser.yaOpenPage('market:offer', {
                                offerId,
                            }));
                        },
                    },
                }),
                prepareSuite(DSBSOffersSuite, {
                    params: {
                        shopName: offerDSBSMock.shop.name,
                        gradesCount: '3 219 отзывов',
                    },
                    hooks: {
                        async beforeEach() {
                            const dsbsOffer = createOffer(offerDSBSMock, offerDSBSMock.id);
                            await this.browser.setState('report', mergeState([
                                dsbsOffer,
                                {
                                    data: {
                                        search: {
                                            total: 1,
                                        },
                                    },
                                },
                            ]));
                            await this.browser.yaOpenPage('market:offer', {
                                offerId: offerDSBSMock.id,
                            });
                        },
                    },
                })
            ),
        }),

        makeSuite('Блок "Товары от магазина."', {
            story: prepareSuite(ShopGoodsSuite, {
                pageObjects: {
                    scrollBox() {
                        return this.createPageObject(ScrollBox, {
                            parent: '[data-zone-data*="Товары от магазина"]',
                        });
                    },
                    shopInfo() {
                        return this.createPageObject(ShopInfo);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', repState);
                        await this.browser.yaOpenPage('market:offer', {offerId: mainOfferId});
                        await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
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
        }),

        makeSuite('Блок "Товары из категории от магазина."', {
            story: prepareSuite(ShopGoodsFromCategorySuite, {
                pageObjects: {
                    scrollBox() {
                        return this.createPageObject(ScrollBox, {
                            parent: '[data-zone-data*="Товары из категории от магазина"]',
                        });
                    },
                    shopInfo() {
                        return this.createPageObject(ShopInfo);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', repState);
                        await this.browser.setState('Cataloger.tree', navState);
                        await this.browser.yaOpenPage('market:offer', {offerId: mainOfferId});
                        await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
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
        }),

        makeSuite('Визитка оффера', {
            story: prepareSuite(OfferSummarySuite, {
                pageObjects: {
                    clickoutButton() {
                        return this.createPageObject(ClickoutButton);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer', {offerId});
                    },
                },
            }),
        }),

        prepareSuite(SearchSimilarReportTextParamSuite, {
            meta: {
                id: 'marketfront-3494',
                issue: 'MARKETVERSTKA-34578',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', offer);
                    await this.browser.yaOpenPage('market:offer-similar', {offerId, text: TEXT_PARAM});
                },
            },
            params: {
                place: 'prime',
                expectedText: TEXT_PARAM,
            },
        }),

        prepareSuite(GeoTextParamSuite, {
            meta: {
                id: 'marketfront-2877',
                issue: 'MARKETVERSTKA-34588',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', offer);
                    await this.browser.yaOpenPage('market:offer-geo', {offerId, text: TEXT_PARAM});
                },
            },
            params: {
                place: 'geo',
                queryParams: [
                    ['show-promoted', '1'],
                    ['regional-delivery', '1'],
                ],
            },
        }),

        prepareSuite(ShopInfoDrugsDisclaimerSuite, {
            meta: {
                id: 'marketfront-3871',
                issue: 'MARKETFRONT-7776',
            },
            pageObjects: {
                shopsInfo() {
                    return this.createPageObject(LegalInfo);
                },
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
                            encrypted: '/redir/encrypted',
                            decrypted: '/redir/decrypted',
                            offercard: '/redir/offercard',
                            geo: '/redir/geo',
                        },
                    }, localOfferId);
                    await this.browser.setState('report', localOffer);
                    await this.browser.yaOpenPage('market:offer', {offerId: localOfferId});
                },
            },
        }),

        makeSuite('Кнопка "В корзину" на оффере', {
            meta: {
                issue: 'MARKETFRONT-13236',
            },
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('Carter.items', []);
                        await this.browser.setState('report', cpaOffer);

                        return this.browser.yaOpenPage('market:offer', {offerId: cpaOfferMock.wareId});
                    },
                },
                prepareSuite(CartButtonSuite),
                prepareSuite(CartButtonCounterSuite),
                prepareSuite(ItemCounterCartButtonSuite, {
                    params: {
                        counterStep: cpaOfferMock.bundleSettings.quantityLimit.step,
                        offerId: cpaOfferMock.wareId,
                    },
                    meta: {
                        id: 'marketfront-4195',
                    },
                    pageObjects: {
                        parent() {
                            return this.createPageObject(ProductDefaultOffer);
                        },
                        cartButton() {
                            return this.createPageObject(CartButton);
                        },
                        counterCartButton() {
                            return this.createPageObject(CounterCartButton);
                        },
                        cartPopup() {
                            return this.createPageObject(CartPopup);
                        },
                    },
                })
            ),
        }),
        {
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
                                page: 'market:offer',
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
                                page: 'market:offer',
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
                                page: 'market:offer',
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
                                page: 'market:offer',
                            });
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
                                page: 'market:offer',
                            });
                        },
                    },
                })
            ),
        },
        prepareSuite(CashbackDealTermSuite, {
            pageObjects: {
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
            },
            meta: {
                id: 'marketfront-4177',
            },
            hooks: {
                async beforeEach() {
                    await createAndSetCashbackState.call(this, false);
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
            suiteName: 'Повышенный кешбэк',
            pageObjects: {
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
            },
            meta: {
                id: 'marketfront-4498',
            },
            hooks: {
                async beforeEach() {
                    await createAndSetCashbackState.call(this, true);
                },
            },
            params: {
                cashbackAmount: CASHBACK_AMOUNT,
                cashbackFormat: 'full',
                isTooltipOnHover: true,
                isExtraCashback: true,
            },
        }),
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
                    await this.browser.yaOpenPage('market:offer', {offerId: primary.offerMock.offerId});
                },
            },
        }),
        prepareSuite(PromoFlashTermSuite, {
            pageObjects: {
                timerFlashSale() {
                    return this.createPageObject(TimerFlashSale, {
                        parent: ProductDefaultOffer.root,
                    });
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
                        state,
                        offerId: wareId,
                    } = prepareKadavrReportState();

                    await this.browser.setState('report', state);
                    await this.browser.setState('Carter.items', []);
                    await this.browser.yaOpenPage('market:offer', {offerId: wareId});
                },
            },
        }),

        prepareSuite(UnitsCalcSuite, {
            suiteName: 'Калькулятор упаковок.',
            pageObjects: {
                unitsCalc() {
                    return this.createPageObject(UnitsCalc, {
                        parent: ProductDefaultOffer.root,
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
                    const {
                        state,
                        offerId: wareId,
                    } = prepareKadavrReportState();

                    await this.browser.setState('report', state);
                    await this.browser.setState(
                        getUnitInfoCollectionPath(wareId),
                        unitInfo
                    );
                    await this.browser.setState(
                        `report.collections.offer.${wareId}.navnodes`,
                        [{
                            ...state.collections.offer[wareId].navnodes[0],
                            tags: ['unit_calc'],
                        }]
                    );
                    await this.browser.setState('Carter.items', []);
                    await this.browser.yaOpenPage('market:offer', {offerId: wareId});
                },
            },
        }),

        prepareSuite(CartPopupUnitsCalcSuite, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        cartButton: () => this.createPageObject(CartButton, {
                            parent: ProductDefaultOffer.root,
                        }),
                        cartPopup: () => this.createPageObject(CartPopup),
                        counterCartButton: () => this.createPageObject(AmountSelect),
                        unitsCalc: () => this.createPageObject(UnitsCalc, {
                            parent: this.cartPopup,
                        }),
                    });

                    const {
                        state,
                        offerId: wareId,
                    } = prepareKadavrReportState();

                    await this.browser.setState('report', state);
                    await this.browser.setState(
                        getUnitInfoCollectionPath(wareId),
                        unitInfo
                    );
                    await this.browser.setState(
                        `report.collections.offer.${wareId}.navnodes`,
                        [{
                            ...state.collections.offer[wareId].navnodes[0],
                            tags: ['unit_calc'],
                        }]
                    );
                    await this.browser.setState('Carter.items', []);
                    await this.browser.yaOpenPage('market:offer', {offerId: wareId});

                    await this.cartButton.click();

                    return this.cartPopup.waitForAppearance();
                },
            },
            pageObjects: {
                unitsCalc() {
                    return this.createPageObject(UnitsCalc, {
                        parent: ProductDefaultOffer.root,
                    });
                },
                productDefaultOffer() {
                    return this.createPageObject(ProductDefaultOffer);
                },
            },
        }),
        prepareSuite(DirectDiscountPromo, {
            pageObjects: {
                directDiscountTerms() {
                    return this.createPageObject(DirectDiscountTerms, {
                        parent: ProductDefaultOffer.root,
                    });
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
                    await this.browser.yaOpenPage('market:offer', {offerId: wareId});
                },
            },
        }),

        prepareSuite(DeliveryBetterWithPlus, {
            params: {
                pageId: 'market:offer',
            },
            pageObjects: {
                freeDeliveryWithPlusLink() {
                    return this.createPageObject(FreeDeliveryWithPlusLink);
                },
            },
        }),

        makeSuite('Фарма', {
            meta: {
                issue: 'marketfront-5217',
            },
            story: mergeSuites(
                {
                    async beforeEach() {
                        const medicineOffer = {
                            ...cpaOfferMock,
                            specs: {
                                internal: [
                                    {
                                        type: 'spec',
                                        value: 'medicine',
                                        usedParams: [],
                                    },
                                ],
                            },
                        };
                        const reportOffer = createOffer(medicineOffer, medicineOffer.wareId);
                        await this.browser.setState('report', reportOffer);

                        return this.browser.yaOpenPage('market:offer', {offerId: medicineOffer.wareId, lr: 213, purchaseList: 1});
                    },
                },
                prepareSuite(PharmaDefaultOfferSuite, {
                    params: {
                        offer: cpaOfferMock,
                    },
                    meta: {
                        id: 'marketfront-5217',
                    },
                    pageObjects: {
                        defaultOffer() {
                            return this.createPageObject(ProductDefaultOffer);
                        },
                        price() {
                            return this.createPageObject(Price, {
                                parent: this.defaultOffer,
                            });
                        },
                        shopInfo() {
                            return this.createPageObject(ShopInfo, {
                                parent: this.defaultOffer,
                            });
                        },
                    },
                })
            ),
        }),

        credit,
        unitInfoDefaultOfferSuite,
        offerWithoutModelSimilar,
        popupComplain,
        disclaimers,
        ageConfirmation,
        metrika,
        shopsInfo,
        tabs,
        bnplSuite,
        offerDegradationSuite
    ),
});

async function createAndSetCashbackState(isExtraCashback) {
    const CASHBACK_PROMO = {
        id: '1',
        key: '1',
        type: 'blue-cashback',
        value: CASHBACK_AMOUNT,
    };

    const EXTRA_CASHBACK_PROMO = {
        id: '2',
        key: '2',
        type: 'blue-cashback',
        value: CASHBACK_AMOUNT,
        tags: ['extra-cashback'],
    };
    const localOfferId = 'uQizLmsYjkLixn5SRhgitQ';
    const localOffer = createOffer({
        entity: 'offer',
        showUid: '15592162537669963376206001',
        id: localOfferId,
        wareId: localOfferId,
        feeShow: 'PAd4m2lIcKCRh4GTCrTeZwM3q2KuJL7JRc9QaZX9tWIL9-lErlJPhLLS77_9LSixoNQJNnYI0EJFJy8xRtSlVhRSwrkABzeK1me4TCjrGhd5ltmPNg9mhqFFd-9YjrueJI22X3adDhbg8iqMn5vr0_UNGp3BO3IKzBw49OqrSIY,',
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
        promos: isExtraCashback ? [EXTRA_CASHBACK_PROMO] : [CASHBACK_PROMO],
    }, localOfferId);
    await this.browser.setState('report', localOffer);
    await this.browser.yaOpenPage('market:offer', {offerId: localOfferId});
}
