// eslint-disable-next-line no-restricted-imports
import {assign} from 'lodash/fp';
import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {
    createProduct,
    createOfferForProduct,
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {getHeadBannerSample, generateKeyByBannerParams} from '@yandex-market/kadavr/mocks/Adfox/helpers';
import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {randomString} from '@self/root/src/helpers/string';

// helpers
import {createProductMock} from '@self/platform/spec/hermione/helpers/product';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {HEADBANNER_ADFOX_PARAMS} from '@self/platform/spec/hermione/configs/banners';
// suites
import virtualPackSuite from '@self/platform/spec/hermione/test-suites/blocks/virtualPacks';
import DefaultOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer';
import DefaultOfferOrderMinCostSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/order-min-cost';
import DefaultOfferValidOfferIdSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/valid-offer-id';
import DefaultOfferInvalidOfferIdSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/invalid-offer-id';
import ProductOffersSnippetOrderMinCostSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/orderMinCost';
import CompareTumblerSuite from '@self/platform/spec/hermione/test-suites/blocks/CompareTumbler';
import VideoCarouselSuite from '@self/platform/spec/hermione/test-suites/blocks/VideoCarousel';
import ReasonsToBuyRecommendSuite from '@self/platform/spec/hermione/test-suites/blocks/ReasonsToBuy/recommend';
import ReasonsToBuyInterestSuite from '@self/platform/spec/hermione/test-suites/blocks/ReasonsToBuy/interest';
import ReasonsToBuyBestSuite from '@self/platform/spec/hermione/test-suites/blocks/ReasonsToBuy/best';
import BreadcrumbsUnifiedSuite from '@self/platform/spec/hermione/test-suites/blocks/breadcrumbsUnified';
import CheckAppearanceSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AuthSuggestionPopup/for-unauth-user/check-appearance';
import CheckParanjaCloseSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AuthSuggestionPopup/for-unauth-user/check-paranja-close';
import CheckLoginSuite from '@self/platform/spec/hermione/test-suites/blocks/AuthSuggestionPopup/for-unauth-user/check-login';
import CheckCancelSuite from '@self/platform/spec/hermione/test-suites/blocks/AuthSuggestionPopup/for-unauth-user/check-cancel';
import ForAuthUserSuite from '@self/platform/spec/hermione/test-suites/blocks/AuthSuggestionPopup/for-auth-user';
import AveragePriceSuite from '@self/platform/spec/hermione/test-suites/blocks/AveragePrice';
import AveragePriceHidden from '@self/platform/spec/hermione/test-suites/blocks/AveragePriceHidden';
import ProductSubscriptionPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup';
import ProductSubscriptionPopupConfirmedFormSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup/confirmedForm';
import ProductSubscriptionPopupEmailConfirmationFormSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup/emailConfirmationForm';
import HeaderAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/Header/absence';
import HeadBannerAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/absence';
import FooterAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/Footer/absence';
import ProductCardUgcSubHeaderWithQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/parts/ProductCard/ProductCardUgcSubHeader/withQuestions';
import ProductCardUgcSubHeaderWithoutQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/parts/ProductCard/ProductCardUgcSubHeader/withoutQuestions';
import ProductCardPopularQuestionsQuestionFormSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductCardPopularQuestions/questionForm';
import QuestionsListQuestionSnippetSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductCardPopularQuestions/QuestionsList/questionSnippet';
import QuestionsListNoAnswersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductCardPopularQuestions/QuestionsList/noAnswers';
import QuestionsListNoQuestionsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductCardPopularQuestions/QuestionsList/noQuestions';
import ShowMoreButtonLessOrEqualThenThreeSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductCardPopularQuestions/ShowMoreButton/lessOrEqualThenThree';
import ShowMoreButtonMoreThanThreeSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductCardPopularQuestions/ShowMoreButton/moreThanThree';
import ProductOffersSortSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffers/sort';
import BaseSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base';
import ProductCardSuite from '@self/platform/spec/hermione/test-suites/blocks/product-card';
import ProductCardQuestionsLinkExistsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/parts/ProductCard/questionsLinkExists';
import FooterSuite from '@self/platform/spec/hermione/test-suites/blocks/Footer';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import MoreOffersDrawer from '@self/platform/spec/hermione/test-suites/blocks/MoreOffersDrawer';
import SimilarTopicsSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSimilarTopics';
import UgcMediaGallerySuite from '@self/platform/spec/hermione/test-suites/blocks/UgcMediaGallery';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import TabsSuite from '@self/platform/spec/hermione/test-suites/blocks/SkuTabs';
import GenericBundleDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import HighratedSimilarProductsSuite from '@self/platform/spec/hermione/test-suites/blocks/HighratedSimilarProducts';
import ProductDegradationSuite from '@self/platform/spec/hermione/test-suites/tops/pages/product/degradation';
import SpreadDiscountCountSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/promos/spreadDiscountCount';
import DirectDiscountPromo from '@self/root/src/spec/hermione/test-suites/blocks/DirectDiscountPromo';
import DeliveryBetterWithPlus
    from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/deliveryBetterWithPlus';
import CartPopupCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartPopup/counter';
import CartPopupCounterUnitsSuite from '@self/platform/spec/hermione/test-suites/blocks/CartPopup/units';
import CartPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/CartPopup';
import UnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc';
import CartPopupUnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/cartPopup';
import CartSinsTunnelingPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/CartSinsTunnelingPopup';

// page-objects
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import ProductCard from '@self/platform/spec/page-objects/product-card';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import OfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import CompareTumbler from '@self/project/src/components/CompareTumbler/__PageObject';
import ProductCompare from '@self/platform/spec/page-objects/ProductCompare';
import Notification from '@self/root/src/components/Notification/__pageObject';
import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';
import AuthSuggestionPopup from '@self/platform/spec/page-objects/widgets/parts/AuthSuggestionPopup';
import AveragePrice from '@self/platform/spec/page-objects/w-average-price';
import ProductSubscriptionPopup from '@self/platform/spec/page-objects/ProductSubscriptionPopup';
import ProductSubscriptionPopupNeedForEmailConfirmationForm from
    '@self/platform/spec/page-objects/ProductSubscriptionPopupNeedForEmailConfirmationForm';
import ProductPage from '@self/platform/spec/page-objects/ProductPage';
import Footer from '@self/platform/spec/page-objects/Footer';
import HeadBanner from '@self/platform/spec/page-objects/HeadBanner';
import Base from '@self/platform/spec/page-objects/n-base';
import ProductOffers from '@self/platform/spec/page-objects/widgets/parts/ProductOffers';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import QuestionForm from '@self/platform/spec/page-objects/QuestionForm';
import QuestionCard from '@self/platform/spec/page-objects/widgets/parts/QuestionsAnswers/QuestionCard';
import ProductCardPopularQuestions from '@self/platform/spec/page-objects/ProductCardPopularQuestions';
import SmallQuestionSnippet from '@self/platform/spec/page-objects/components/Questions/SmallQuestionSnippet';
import ProductCardHeader from '@self/platform/spec/page-objects/ProductCardHeader';
import OfferModifications from '@self/platform/spec/page-objects/widgets/content/OfferModifications';
import GalleryUgcSlider from '@self/platform/components/Gallery/GalleryUgcSlider/__pageObject';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import ProductCardLinks from '@self/platform/widgets/parts/ProductCardLinks/__pageObject';
import DealsSticker from '@self/platform/spec/page-objects/DealsSticker';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import HighratedSimilarProducts from '@self/platform/spec/page-objects/widgets/content/HighratedSimilarProducts';
import DefaultOfferPrice from '@self/platform/components/DefaultOffer/PriceInfo/Price/__pageObject';
import DirectDiscountTerms from '@self/root/src/components/DirectDiscountTerms/__pageObject';
import FreeDeliveryWithPlusLink from '@self/root/src/components/FreeDeliveryWithPlusLink/__pageObject';
import CategorySnippet from '@self/root/src/widgets/content/CategorySnippet/__pageObject';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';
import bannerAfterReverseScroll from '@self/root/src/spec/hermione/test-suites/touch.blocks/bannerAfterReverseScroll';
import SearchHeaderRedesigned from '@self/platform/widgets/content/SearchHeader/redesign/__pageObject';
import WidgetWrapper from '@self/root/src/components/WidgetWrapper/__pageObject';
import UnitsCalc from '@self/root/src/components/UnitsCalc/__pageObject';

// helpers
import {hideSmartBannerPopup} from '@self/platform/spec/hermione/helpers/smartBannerPopup';
// fixtures
import {
    defaultOfferWithCpaOptions as defaultOfferWithCpaMock,
    productWithDefaultOffer,
    productWithReasons,
    productWithPicture,
    productClusterWithPicture,
    productWithCPADefaultOffer,
    phoneProductRoute,
    productWithAveragePrice,
    productWithAveragePriceChartInfo,
    productWithDefaultCashbackOffer,
    productWithDefaultExtraCashbackOffer,
    CASHBACK_AMOUNT,
    DEFAULT_OFFER_WARE_ID,
    productWithCPADefaultOfferAndUnitInfo,
} from '@self/platform/spec/hermione/fixtures/product';
import {createUser} from '@self/platform/spec/hermione/fixtures/user';
import {createQuestion} from '@self/root/src/spec/hermione/fixtures/question';
import {createUserReviewWithPhotos} from '@self/platform/spec/hermione/fixtures/review';
import {cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import PromoFlashTermSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoFlash';
import {prepareKadavrReportState} from '@self/project/src/spec/hermione/fixtures/promo/flash';
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import {spreadDiscountCountPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';
import PromoFlashDescription from '@self/project/src/components/BlueFlashDescription/__pageObject';
import TimerFlashSale from '@self/project/src/components/TimerFlashSale/__pageObject';
import {promoText as directDiscountText, prepareKadavrReportStateForDirectDiscount} from '@self/project/src/spec/hermione/fixtures/promo/directDiscount';
import productWithCpaDo from '@self/platform/spec/hermione/fixtures/product/productWithCpaDo';
import {productStateWithFormula, formulaCatalogerMock} from '@self/platform/spec/hermione/test-suites/blocks/ProductSimilarTopics/fixtures';
import breadcrumbsCatalogerMock from './breadcrumbs-cataloger-mock.json';

// imports
import preorder from './preorder';
import subscription from './subscription';
import ageConfirmation from './ageConfirmation';
import vendorLine from './vendorLine';
import credit from './credit';
import deals from './deals';
import upsale from './spVendors/upsale';
import promoBadge from './spVendors/promoBadge';
import drugsDisclaimer from './drugsDisclaimer';
import gallery from './gallery';
import filters from './filters';
import embeddedFilters from './embeddedFilters';
import allFiltersPage from './allFiltersPage';
import automaticallyCalculatedDelivery from './automaticallyCalculatedDelivery';
import russianPost from './russianPost';
import defaultOfferDelivery from './delivery';
import videoUrlsFixture from '../fixtures/videoUrls';
import {testShop, anotherTestShop, offerUrls, defaultQuestion, category} from './kadavrMocks';
import {buildProductOffersResultsState, PRODUCT_ROUTE, SHOP_INFO} from './fixtures/productWithMoreOffers';
import {buildColorFilter, createFilterIds} from './fixtures/productWithVisualFilters';
import linkSellersInfo from './linkSellersInfo';
import linkSellersInfoNotInStock from './linkSellersInfoNotInStock';
import productReviewFormMicro from './productReviewFormMicro';
import productReviewsScrollBox from './productReviewsScrollBox';
import placementTypes from './placementTypes/defaultOffer';
import expressDefaultOffer from './express/defaultOffer';
import secondaryExpressOffer from './express/secondaryExpressOffer';
import {bnplSuite} from './bnpl';
import defaultOffer from './unitInfo/defaultOffer';
import topOffers from './unitInfo/topOffers';
import digitalOfferSuite from './digital';
import uniqueOfferSuite from './unique';
import estimatedOfferSuite from './estimated';

const PHONE_ROUTE_CONFIG = routes.product.phone;
const DEFAULT_USER = createUser({userUid: profiles.ugctest3.uid});
const DEFAULT_QUESTION = createQuestion({
    productId: PHONE_ROUTE_CONFIG.productId,
    userUid: DEFAULT_USER.id,
});
const DEFAULT_FILTER_VALUE_ID = 1;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Морда карточки модели.', {
    environment: 'testing',
    story: {
        async beforeEach() {
            await hideSmartBannerPopup(this);
        },
        'Карточка модели с параметром sku.': mergeSuites({
            async beforeEach() {
                const config = routes.product.videoCardPalit;

                const videocardProductOptions = {
                    type: 'model',
                    description: 'Ящик пандоры',
                    filters: [{
                        id: 14871214,
                        type: 'enum',
                        name: 'Цвет товара',
                        xslname: 'color_vendor',
                        subType: 'image_picker',
                        kind: 2,
                        position: 1,
                        noffers: 1,
                        valuesCount: 1,
                        values: [{
                            initialFound: 1,
                            checked: true,
                            found: 1,
                            value: 'золотой',
                            id: 15266392,
                        }],
                    }],
                    categories: [
                        {
                            entity: 'category',
                            id: config.hid,
                            name: 'Видеокарты',
                            fullName: 'Видеокарты',
                            type: 'guru',
                            slug: 'videokarty',
                            isLeaf: true,
                        },
                    ],
                    vendor: {
                        name: 'Palit',
                        filter: '7893318:779586',
                    },
                };
                const reportState = createProduct(videocardProductOptions, String(config.productId));
                await this.browser.setState('report', reportState);
                await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                await this.browser.yaOpenPage(
                    'touch:product',
                    Object.assign({}, config, {sku: 1111})
                );
            },
        },
        prepareSuite(TabsSuite)
        ),
        'Карточка модели.': mergeSuites(
            makeSuite('Дефолтный офффер, указанный в ссылке через параметр do-waremd5', {
                environment: 'kadavr',
                issue: 'MARKETFRONT-34303',
                story: mergeSuites(
                    prepareSuite(DefaultOfferValidOfferIdSuite, {
                        hooks: {
                            async beforeEach() {
                                const filterValueIds = createFilterIds(5);
                                const colorFilter = buildColorFilter(filterValueIds, id => (
                                    id === DEFAULT_FILTER_VALUE_ID ? {checked: true} : {}
                                ));
                                const wareMd5Offer = createOffer({
                                    ...cpaOfferMock,
                                    benefit: {
                                        type: 'waremd5',
                                        isPrimary: true,
                                    },
                                }, cpaOfferMock.wareId);
                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 2,
                                            totalOffers: 2,
                                        },
                                    },
                                };

                                await this.browser.setState('report', mergeState([
                                    productWithDefaultOffer,
                                    wareMd5Offer,
                                    colorFilter,
                                    dataMixin,
                                ]));
                                await this.browser.yaOpenPage('touch:product', {
                                    ...phoneProductRoute,
                                    'do-waremd5': cpaOfferMock.wareId,
                                });
                            },
                        },
                        params: {
                            expectedOfferId: cpaOfferMock.wareId,
                            defaultFilterValueId: DEFAULT_FILTER_VALUE_ID,
                            nextFilterValueId: 2,
                        },
                    }),
                    prepareSuite(DefaultOfferInvalidOfferIdSuite, {
                        hooks: {
                            async beforeEach() {
                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                await this.browser.setState('report', mergeState([
                                    productWithDefaultOffer,
                                    dataMixin,
                                ]));
                                await this.browser.yaOpenPage('touch:product', {
                                    ...phoneProductRoute,
                                    'do-waremd5': cpaOfferMock.wareId,
                                });
                            },
                        },
                        params: {
                            expectedOfferId: DEFAULT_OFFER_WARE_ID,
                        },
                    })
                ),
            }),
            makeSuite('Дефолтный оффер', {
                environment: 'kadavr',
                story: mergeSuites(
                    prepareSuite(DefaultOfferSuite, {
                        pageObjects: {
                            defaultOffer() {
                                return this.createPageObject(DefaultOffer);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                await this.browser.setState('report', mergeState([
                                    productWithDefaultOffer,
                                    dataMixin,
                                ]));

                                await this.browser.yaSetCookie({
                                    name: 'currentRegionId',
                                    value: '213',
                                });

                                await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                            },
                        },
                    }),
                    prepareSuite(UnitsCalcSuite, {
                        suiteName: 'Калькулятор упаковок.',
                        params: {
                            expectedText: '1 уп = 1.248 м²',
                        },
                        meta: {
                            id: 'marketfront-5766',
                            issue: 'MARKETFRONT-79800',
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', mergeState([
                                    productWithCPADefaultOfferAndUnitInfo,
                                ]));

                                await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                            },
                        },
                        pageObjects: {
                            unitsCalc() {
                                return this.createPageObject(UnitsCalc, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                    }),
                    prepareSuite(CartPopupUnitsCalcSuite, {
                        hooks: {
                            async beforeEach() {
                                this.setPageObjects({
                                    cartButton: () => this.createPageObject(CartButton, {
                                        parent: DefaultOffer.root,
                                    }),
                                    cartPopup: () => this.createPageObject(CartPopup),
                                    counterCartButton: () => this.createPageObject(CounterCartButton, {
                                        parent: this.cartPopup,
                                    }),
                                    unitsCalc: () => this.createPageObject(UnitsCalc, {
                                        parent: this.cartPopup,
                                    }),
                                });

                                await this.browser.setState('report', mergeState([
                                    productWithCPADefaultOfferAndUnitInfo,
                                ]));

                                await this.browser.yaOpenPage('touch:product', phoneProductRoute);

                                await this.browser.scroll(CartButton.root);
                                await this.cartButton.click();

                                await this.browser.waitForVisible(CartPopup.root, 10000);
                            },
                        },
                        pageObjects: {
                            unitsCalc() {
                                return this.createPageObject(UnitsCalc, {
                                    parent: DefaultOffer.root,
                                });
                            },
                            productDefaultOffer() {
                                return this.createPageObject(DefaultOffer);
                            },
                        },
                    })
                ),
            }),

            makeSuite('Дефолтный оффер. CPA', {
                environment: 'kadavr',
                story: mergeSuites(
                    {
                        async beforeEach() {
                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                    },
                                },
                            };

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', mergeState([
                                productWithCPADefaultOffer,
                                dataMixin,
                            ]));
                            await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        },
                    },
                    prepareSuite(CartButtonSuite, {
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                    }),
                    prepareSuite(CartButtonCounterSuite, {
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                    }),
                    prepareSuite(ItemCounterCartButtonSuite, {
                        meta: {
                            id: 'm-touch-3430',
                        },
                        params: {
                            counterStep: defaultOfferWithCpaMock.bundleSettings.quantityLimit.step,
                            offerId: defaultOfferWithCpaMock.wareId,
                        },
                        pageObjects: {
                            parent() {
                                return this.createPageObject(DefaultOffer);
                            },
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                    }),
                    prepareSuite(SpreadDiscountCountSuite, {
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                            price() {
                                return this.createPageObject(DefaultOfferPrice, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                const offer = createOffer({
                                    ...productWithCPADO.offerMock,
                                    promos: [spreadDiscountCountPromo],
                                }, productWithCPADO.offerMock.wareId);
                                const reportState = mergeState([
                                    productWithCPADO.product,
                                    offer,
                                    {
                                        data: {
                                            search: {
                                                total: 1,
                                            },
                                        },
                                    },
                                ]);
                                await this.browser.setState('report', reportState);
                                await this.browser.yaOpenPage('touch:product', productWithCPADO.route);

                                return this.cartButton.click();
                            },
                        },
                        params: {
                            promoBound: spreadDiscountCountPromo.itemsInfo.bounds[0],
                        },
                    }),
                    {
                        'Блок кешбэка': mergeSuites(
                            {
                                async beforeEach() {
                                    this.setPageObjects({
                                        cashbackDealTerms() {
                                            return this.createPageObject(CashbackDealTerms, {
                                                parent: DefaultOffer.root,
                                            });
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
                                    id: 'marketfront-4178',
                                },
                                hooks: {
                                    async beforeEach() {
                                        const dataMixin = {
                                            data: {
                                                search: {
                                                    total: 1,
                                                    totalOffers: 1,
                                                },
                                            },
                                        };
                                        await this.browser.setState('report', mergeState([
                                            productWithDefaultCashbackOffer,
                                            dataMixin,
                                        ]));
                                        await this.browser.yaLogin(
                                            profiles['pan-topinambur'].login,
                                            profiles['pan-topinambur'].password
                                        );
                                        return this.browser.yaOpenPage('touch:product', phoneProductRoute);
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
                                meta: {
                                    id: 'marketfront-4499',
                                },
                                hooks: {
                                    async beforeEach() {
                                        const dataMixin = {
                                            data: {
                                                search: {
                                                    total: 1,
                                                    totalOffers: 1,
                                                },
                                            },
                                        };
                                        await this.browser.setState('report', mergeState([
                                            productWithDefaultExtraCashbackOffer,
                                            dataMixin,
                                        ]));
                                        await this.browser.yaLogin(
                                            profiles['pan-topinambur'].login,
                                            profiles['pan-topinambur'].password
                                        );
                                        return this.browser.yaOpenPage('touch:product', phoneProductRoute);
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
                    },
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
                            id: 'marketfront-4258',
                        },
                        hooks: {
                            async beforeEach() {
                                const {
                                    stateWithProductOffers,
                                    primary,
                                } = prepareKadavrReportStateWithDefaultState();

                                await this.browser.setState('report', stateWithProductOffers);
                                await this.browser.setState('Carter.items', []);
                                return this.browser.yaOpenPage('touch:product', {
                                    productId: primary.productMock.id,
                                    slug: primary.productMock.navnodes[0].slug,
                                });
                            },
                        },
                    }),
                    prepareSuite(PromoFlashTermSuite, {
                        pageObjects: {
                            timerFlashSale() {
                                return this.createPageObject(TimerFlashSale, {
                                    parent: DefaultOffer.root,
                                });
                            },
                            promoFlashDescription() {
                                return this.createPageObject(PromoFlashDescription);
                            },
                        },
                        meta: {
                            id: 'marketfront-4292',
                        },
                        hooks: {
                            async beforeEach() {
                                const {
                                    state,
                                    productId,
                                    slug,
                                } = prepareKadavrReportState();

                                await this.browser.setState('report', state);
                                await this.browser.setState('Carter.items', []);
                                return this.browser.yaOpenPage('touch:product', {
                                    productId,
                                    slug,
                                });
                            },
                        },
                    }),
                    prepareSuite(DirectDiscountPromo, {
                        pageObjects: {
                            directDiscountTerms() {
                                return this.createPageObject(DirectDiscountTerms, {
                                    parent: DefaultOffer.root,
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
                                    productId,
                                    slug,
                                } = prepareKadavrReportStateForDirectDiscount();

                                await this.browser.setState('report', state);
                                await this.browser.setState('Carter.items', []);
                                return this.browser.yaOpenPage('touch:product', {
                                    productId,
                                    slug,
                                });
                            },
                        },
                    }),
                    prepareSuite(CartPopupSuite, {
                        meta: {
                            environment: 'kadavr',
                            id: 'm-touch-3690',
                            issue: 'MARKETFRONT-52899',
                        },
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                        params: {
                            createReportMock: offerParams => mergeReportState([productWithPicture, createOffer({
                                ...cpaOfferMock,
                                urls: {
                                    cpa: '/redir/cpa',
                                },
                                benefit: {type: 'default', description: '', params: {}, isPrimary: true},
                                ...offerParams,
                            }, cpaOfferMock.wareId), {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                        totalOffersBeforeFilters: 1,
                                        totalModels: 0,
                                    },
                                },
                            }]),
                            pageId: 'touch:product',
                            routeParams: phoneProductRoute,
                        },
                    }),
                    prepareSuite(CartPopupCounterSuite, {
                        meta: {
                            environment: 'kadavr',
                            id: 'm-touch-3691',
                            issue: 'MARKETFRONT-52899',
                        },
                        pageObjects: {
                            popupCartCounter() {
                                return this.createPageObject(CounterCartButton, {
                                    parent: CartPopup.root,
                                });
                            },

                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: DefaultOffer.root,
                                });
                            },
                        },
                    })

                ),
            }),

            makeSuite('Топ-6 с CPA-оффером', {
                environment: 'kadavr',
                story: mergeSuites(
                    {
                        async beforeEach() {
                            const offers = [];
                            const offersCount = 4;

                            for (let i = 0; i < offersCount - 1; i++) {
                                offers.push(createOffer({
                                    urls: offerUrls,
                                }));
                            }

                            offers.push(createOffer({
                                ...cpaOfferMock,
                                urls: {
                                    cpa: '/redir/cpa',
                                },
                            }, cpaOfferMock.wareId));

                            const state = mergeReportState([productWithPicture, ...offers, {
                                data: {
                                    search: {
                                        total: offersCount,
                                        totalOffers: offersCount,
                                        totalOffersBeforeFilters: offersCount,
                                        totalModels: 0,
                                    },
                                },
                            }]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        },
                    },
                    prepareSuite(CartButtonSuite, {
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: `${ProductOffers.root}:first-child`,
                                });
                            },
                        },
                    }),
                    prepareSuite(CartButtonCounterSuite, {
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: `${ProductOffers.root}:first-child`,
                                });
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
                                return this.createPageObject(ProductOffers);
                            },
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: `${ProductOffers.root}:first-child`,
                                });
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton, {
                                    parent: `${ProductOffers.root}:first-child`,
                                });
                            },
                        },
                    }),
                    prepareSuite(CartPopupSuite, {
                        meta: {
                            environment: 'kadavr',
                            id: 'm-touch-3692',
                            issue: 'MARKETFRONT-52899',
                        },
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: `${ProductOffers.root}:first-child`,
                                });
                            },
                        },
                        params: {
                            createReportMock: offerParams => {
                                const offers = [];
                                const offersCount = 4;

                                for (let i = 0; i < offersCount - 1; i++) {
                                    offers.push(createOffer({
                                        urls: offerUrls,
                                    }));
                                }

                                offers.push(createOffer({
                                    ...cpaOfferMock,
                                    urls: {
                                        cpa: '/redir/cpa',
                                    },
                                    ...offerParams,
                                }, cpaOfferMock.wareId));

                                return mergeReportState([productWithPicture, ...offers, {
                                    data: {
                                        search: {
                                            total: offersCount,
                                            totalOffers: offersCount,
                                            totalOffersBeforeFilters: offersCount,
                                            totalModels: 0,
                                        },
                                    },
                                }]);
                            },
                            pageId: 'touch:product',
                            routeParams: phoneProductRoute,
                        },
                    }),
                    {'Страница цен.': prepareSuite(CartPopupSuite, {
                        pageObjects: {
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: `${ProductOffers.root}:first-child`,
                                });
                            },
                        },
                        params: {
                            createReportMock: offerParams => {
                                const state = mergeReportState([
                                    productWithCpaDo.state,
                                    {collections: {offer: {
                                        789: {benefit: null},
                                        [cpaOfferMock.wareId]: {...cpaOfferMock, ...offerParams}},
                                    }},
                                ]);
                                state.data.search.results.unshift({id: cpaOfferMock.wareId, schema: 'offer'});

                                return state;
                            },
                            pageId: 'touch:product-offers',
                            routeParams: productWithCpaDo.route,
                        },
                    })},
                    {
                        'Блок кешбэка': mergeSuites(
                            {
                                async beforeEach() {
                                    this.setPageObjects({
                                        cashbackDealTerms() {
                                            return this.createPageObject(CashbackDealTerms, {
                                                parent: `${ProductOffers.root}:first-child`,
                                            });
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
                                    id: 'marketfront-4179',
                                },
                                hooks: {
                                    async beforeEach() {
                                        const offers = [];
                                        const offersCount = 2;
                                        for (let i = 0; i < offersCount; i++) {
                                            offers.push(createOffer({
                                                urls: offerUrls,
                                                promos: [{
                                                    type: 'blue-cashback',
                                                    value: CASHBACK_AMOUNT,
                                                }],
                                            }));
                                        }
                                        const state = mergeReportState([productWithPicture, ...offers, {
                                            data: {
                                                search: {
                                                    total: offersCount,
                                                    totalOffers: offersCount,
                                                    totalOffersBeforeFilters: offersCount,
                                                    totalModels: 0,
                                                },
                                            },
                                        }]);
                                        await this.browser.setState('Carter.items', []);
                                        await this.browser.setState('report', state);
                                        await this.browser.yaLogin(
                                            profiles['pan-topinambur'].login,
                                            profiles['pan-topinambur'].password
                                        );
                                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                                    },
                                },
                                params: {
                                    cashbackAmount: 100,
                                    cashbackFormat: 'full',
                                    isTooltipOnHover: true,
                                    isExtraCashback: false,
                                },
                            }),
                            prepareSuite(CashbackDealTermSuite, {
                                suiteName: 'Повышенный кешбэк.',
                                meta: {
                                    id: 'marketfront-4499',
                                },
                                hooks: {
                                    async beforeEach() {
                                        const offers = [];
                                        const offersCount = 2;
                                        for (let i = 0; i < offersCount; i++) {
                                            offers.push(createOffer({
                                                urls: offerUrls,
                                                promos: [{
                                                    type: 'blue-cashback',
                                                    value: CASHBACK_AMOUNT,
                                                    tags: ['extra-cashback'],
                                                }],
                                            }));
                                        }
                                        const state = mergeReportState([productWithPicture, ...offers, {
                                            data: {
                                                search: {
                                                    total: offersCount,
                                                    totalOffers: offersCount,
                                                    totalOffersBeforeFilters: offersCount,
                                                    totalModels: 0,
                                                },
                                            },
                                        }]);
                                        await this.browser.setState('Carter.items', []);
                                        await this.browser.setState('report', state);
                                        await this.browser.yaLogin(
                                            profiles['pan-topinambur'].login,
                                            profiles['pan-topinambur'].password
                                        );
                                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                                    },
                                },
                                params: {
                                    cashbackAmount: 100,
                                    cashbackFormat: 'full',
                                    isTooltipOnHover: true,
                                    isExtraCashback: true,
                                },
                            })
                        ),
                    }
                ),
            }),

            makeSuite('Минимальная сумма заказа.', {
                environment: 'kadavr',
                story: mergeSuites(
                    prepareSuite(DefaultOfferOrderMinCostSuite, {
                        pageObjects: {
                            defaultOffer() {
                                return this.createPageObject(DefaultOffer);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                await this.browser.setState('report', mergeState([
                                    productWithDefaultOffer,
                                    dataMixin,
                                ]));

                                return this.browser.yaOpenPage('touch:product', phoneProductRoute);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetOrderMinCostSuite, {
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(OfferSnippet);
                            },
                        },
                        hooks: {
                            beforeEach() {
                                const offerRegular = createOfferForProduct(
                                    {
                                        urls: offerUrls,
                                        shop: testShop,
                                        orderMinCost: {
                                            value: 5500,
                                            currency: 'RUR',
                                        },
                                    },
                                    phoneProductRoute.productId,
                                    2
                                );
                                const offerRegular2 = createOfferForProduct(
                                    {
                                        urls: offerUrls,
                                        shop: anotherTestShop,
                                        orderMinCost: {
                                            value: 5500,
                                            currency: 'RUR',
                                        },
                                    },
                                    phoneProductRoute.productId,
                                    3
                                );
                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 2,
                                            totalOffers: 2,
                                            totalOffersBeforeFilters: 2,
                                            results: [
                                                {
                                                    id: '2',
                                                    schema: 'offer',
                                                },
                                                {
                                                    id: '3',
                                                    schema: 'offer',
                                                },
                                            ],
                                        },
                                    },
                                };
                                const reportState = mergeReportState([
                                    productWithPicture,
                                    offerRegular,
                                    offerRegular2,
                                    dataMixin,
                                ]);
                                return this.browser.setState('report', reportState)
                                    .then(() => this.browser.yaOpenPage('touch:product', phoneProductRoute));
                            },
                        },
                    })
                ),
            }),

            prepareSuite(CompareTumblerSuite, {
                pageObjects: {
                    compareTumbler() {
                        return this.createPageObject(CompareTumbler);
                    },
                    productCompare() {
                        return this.createPageObject(ProductCompare);
                    },
                    notification() {
                        return this.createPageObject(Notification);
                    },
                },
                hooks: {
                    beforeEach() {
                        const params = routes.product.dress;
                        return this.browser
                            .yaOpenPage('touch:product', params)
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
            }),

            makeSuite('Видеообзоры.', {
                environment: 'kadavr',
                story: prepareSuite(VideoCarouselSuite, {
                    hooks: {
                        async beforeEach() {
                            this.params.expectedUrl = '/product--smartfon-apple-iphone-7-128gb/14206682/videos';

                            await this.browser.setState('report', productWithPicture);
                            await this.browser.setState('Tarantino.data.result', videoUrlsFixture);
                            await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        },
                    },
                    pageObjects: {
                        scrollBox() {
                            return this.createPageObject(ScrollBox, {
                                parent: ProductPage.videoCarousel,
                            });
                        },
                    },
                }),
            }),

            makeSuite('Причины купить товар.', {
                environment: 'kadavr',
                story: mergeSuites(
                    {
                        async beforeEach() {
                            await this.browser.setState('report', productWithReasons);

                            await this.browser.yaOpenPage('touch:product', routes.product.phoneWithAveragePrice);

                            await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                        },
                    },
                    makeSuite('Бейдж "Выбор покупателей"', {
                        id: 'm-touch-2095',
                        issue: 'MOBMARKET-8031',
                        story: prepareSuite(ReasonsToBuyRecommendSuite),
                    }),
                    makeSuite('Бейдж "Этот товар купили N человека"', {
                        id: 'm-touch-2107',
                        issue: 'MOBMARKET-8066',
                        story: prepareSuite(ReasonsToBuyInterestSuite),
                    }),
                    makeSuite('Бейдж "Покупателям нравится"', {
                        id: 'm-touch-2096',
                        issue: 'MOBMARKET-8032',
                        story: prepareSuite(ReasonsToBuyBestSuite),
                    })
                ),
            }),

            makeSuite('Переходы по хлебным крошкам.', {
                environment: 'kadavr',
                story: prepareSuite(BreadcrumbsUnifiedSuite, {
                    meta: {
                        id: 'm-touch-2306',
                        issue: 'MOBMARKET-9240',
                    },
                    pageObjects: {
                        breadcrumbsUnified() {
                            return this.createPageObject(BreadcrumbsUnified);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const config = routes.product.videoCardPalit;

                            const videocardProductOptions = {
                                type: 'model',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: config.hid,
                                        name: 'Видеокарты',
                                        fullName: 'Видеокарты',
                                        type: 'guru',
                                        slug: 'videokarty',
                                        isLeaf: true,
                                    },
                                ],
                                vendor: {
                                    name: 'Palit',
                                    slug: 'palit',
                                    filter: '7893318:779586',
                                },
                            };
                            const {slug: firstCrumbSlug, link: {params: firstCrumbParams}} = breadcrumbsCatalogerMock.navnodes[0];
                            const {slug: secondCrumbSlug, link: {params: secondCrumbParams}} = breadcrumbsCatalogerMock.navnodes[0].navnodes[0];
                            const {slug: vendorSlug, filter: vendorFilterParam} = videocardProductOptions.vendor;
                            const reportState = createProduct(videocardProductOptions, String(config.productId));
                            await this.browser.setState('report', reportState);
                            await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                            await this.browser.yaOpenPage('touch:product', config);
                            await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                            this.params = assign(
                                {
                                    links: [
                                        {pathname: `/catalog--${firstCrumbSlug}/${firstCrumbParams.nid[0]}`, query: {hid: firstCrumbParams.hid[0]}},
                                        {pathname: `/catalog--${secondCrumbSlug}/${secondCrumbParams.nid[0]}/list`, query: {hid: secondCrumbParams.hid[0]}},
                                        {pathname: `/catalog--${vendorSlug}/${secondCrumbParams.nid[0]}/list`, query: {hid: secondCrumbParams.hid[0], glfilter: vendorFilterParam}},
                                    ],
                                },
                                this.params
                            );
                        },
                    },
                }),
            }),
            {
                'Кнопка сравнения.': {
                    async beforeEach() {
                        await this.browser.setState('report', productWithPicture);
                    },
                    'Для неавторизованного юзера.': mergeSuites({
                        async beforeEach() {
                            this.setPageObjects({
                                tumbler: () => this.createPageObject(CompareTumbler),
                                authSuggestionPopup: () =>
                                    this.createPageObject(AuthSuggestionPopup),
                            });

                            return this.browser
                                .yaOpenPage('touch:product', phoneProductRoute)
                                // .then(() => this.browser.waitForVisible('hdfjahfkj', 50000000))
                                .then(() => this.browser.allure.runStep(
                                    'Чистим куку для показа попап авторизации',
                                    () => this.browser.deleteCookie('authSuggestion'))
                                )
                                .yaReactPageReload(10000);
                        },

                        afterEach() {
                            return this.browser.deleteCookie('authSuggestion');
                        },
                    },
                    prepareSuite(CheckAppearanceSuite, {
                        meta: {
                            id: 'm-touch-1964',
                            issue: 'MOBMARKET-7732',
                        },
                        params: {
                            description: 'Войдите, чтобы сохранить сравнение товаров',
                        },
                    }),
                    prepareSuite(CheckParanjaCloseSuite, {
                        meta: {
                            id: 'm-touch-1967',
                            issue: 'MOBMARKET-7735',
                        },
                    }),
                    prepareSuite(CheckLoginSuite, {
                        meta: {
                            id: 'm-touch-1966',
                            issue: 'MOBMARKET-7734',
                        },
                    }),
                    prepareSuite(CheckCancelSuite, {
                        meta: {
                            id: 'm-touch-1965',
                            issue: 'MOBMARKET-7733',
                        },
                    })),

                    'Для авторизованного юзера.': prepareSuite(ForAuthUserSuite, {
                        meta: {
                            id: 'm-touch-1969',
                            issue: 'MOBMARKET-7737',
                        },
                        pageObjects: {
                            tumbler() {
                                return this.createPageObject(CompareTumbler);
                            },
                            authSuggestionPopup() {
                                return this.createPageObject(AuthSuggestionPopup);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                const retPath = await this.browser.yaBuildURL('touch:product', phoneProductRoute);

                                return this.browser
                                    .yaOpenPage('touch:product', phoneProductRoute)
                                    .then(() => this.browser.allure.runStep(
                                        'Чистим куку для показа попап авторизации',
                                        () => this.browser.deleteCookie('authSuggestion'))
                                    )
                                    .then(() => this.browser.allure.runStep(
                                        'Логинимся', () => this.browser.yaTestLogin(retPath))
                                    )
                                    .yaReactPageReload(10000);
                            },
                            afterEach() {
                                return this.browser
                                    .deleteCookie('authSuggestion')
                                    .yaLogout();
                            },
                        },
                    }),
                },
            },

            makeSuite('Средняя цена.', {
                environment: 'kadavr',
                story: {
                    'Не отображается при отсутствие информации о средней цене товара': mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('PriceChart.info', {});
                                await this.browser.setState('report', productWithAveragePrice);

                                this.setPageObjects({
                                    averagePrice: () => this.createPageObject(AveragePrice),
                                    productSubscriptionPopup: () => this.createPageObject(ProductSubscriptionPopup),
                                    needForEmailConfirmationForm: () =>
                                        this.createPageObject(ProductSubscriptionPopupNeedForEmailConfirmationForm),
                                });

                                await this.browser.yaLogout();

                                await this.browser.yaOpenPage('touch:product', routes.product.phoneWithAveragePrice);

                                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },
                        prepareSuite(AveragePriceHidden)
                    ),
                    'Отображается': mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('PriceChart.info', {
                                    [routes.product.phoneWithAveragePrice.productId]: productWithAveragePriceChartInfo,
                                });
                                await this.browser.setState('report', productWithAveragePrice);

                                this.setPageObjects({
                                    averagePrice: () => this.createPageObject(AveragePrice),
                                    productSubscriptionPopup: () => this.createPageObject(ProductSubscriptionPopup),
                                    needForEmailConfirmationForm: () =>
                                        this.createPageObject(ProductSubscriptionPopupNeedForEmailConfirmationForm),
                                });

                                await this.browser.yaLogout();

                                await this.browser.yaOpenPage('touch:product', routes.product.phoneWithAveragePrice);

                                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                            },
                        },

                        prepareSuite(AveragePriceSuite),
                        prepareSuite(ProductSubscriptionPopupSuite),

                        {
                            'Для неавторизованного пользователя.':
                            prepareSuite(ProductSubscriptionPopupEmailConfirmationFormSuite, {
                                id: 'm-touch-2249',
                                issue: 'MOBMARKET-8971',
                            }),

                            'Для авторизованного пользователя.': mergeSuites({
                                async beforeEach() {
                                    const retPath = await this.browser
                                        .yaBuildURL('touch:product', routes.product.phoneWithAveragePrice);

                                    return this.browser.allure.runStep('Логинимся',
                                        () => this.browser.yaTestLogin(retPath));
                                },

                                afterEach() {
                                    return this.browser.yaLogout();
                                },
                            },
                            prepareSuite(ProductSubscriptionPopupConfirmedFormSuite, {
                                meta: {
                                    id: 'm-touch-2250',
                                    issue: 'MOBMARKET-8972',
                                },
                            }),
                            prepareSuite(ProductSubscriptionPopupEmailConfirmationFormSuite, {
                                meta: {
                                    id: 'm-touch-2357',
                                    issue: 'MOBMARKET-9535',
                                },
                            })
                            ),
                        }
                    ),
                },
            }),

            makeSuite('Все параметры', {
                environment: 'kadavr',
                story: mergeSuites(
                    {
                        async beforeEach() {
                            const offerRegular = createOfferForProduct(
                                {
                                    urls: offerUrls,
                                    shop: testShop,
                                    orderMinCost: {
                                        value: 5500,
                                        currency: 'RUR',
                                    },
                                },
                                phoneProductRoute.productId,
                                2
                            );
                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                        totalOffersBeforeFilters: 1,
                                        results: [
                                            {
                                                id: '2',
                                                schema: 'offer',
                                            },
                                        ],
                                    },
                                },
                            };
                            const reportState = mergeReportState([
                                productWithPicture,
                                offerRegular,
                                dataMixin,
                            ]);
                            await this.browser.setState('report', reportState);

                            const headBannerKey = generateKeyByBannerParams(HEADBANNER_ADFOX_PARAMS);
                            const adfoxState = {
                                [headBannerKey]: Object.assign(
                                    {},
                                    getHeadBannerSample(),
                                    {ad_place: HEADBANNER_ADFOX_PARAMS.id}
                                ),
                            };

                            await this.browser.setState('Adfox.data.collections.banners', adfoxState);

                            this.setPageObjects({
                                productPage: () => this.createPageObject(ProductPage),
                            });

                            await this.browser.yaLogout();

                            await this.browser.yaOpenPage('touch:product', phoneProductRoute);

                            await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                            await this.browser.allure.runStep('Нажимаем "Подобрать по параметрам"', () =>
                                this.productPage.allParameters.click()
                            );
                        },
                    },

                    prepareSuite(HeaderAbsenceSuite, {
                        pageObjects: {
                            header() {
                                return this.createPageObject(Header);
                            },
                        },
                    }),

                    prepareSuite(HeadBannerAbsenceSuite, {
                        meta: {
                            id: 'm-touch-2379',
                            issue: 'MOBMARKET-9655',
                        },
                        pageObjects: {
                            headBanner() {
                                return this.createPageObject(HeadBanner);
                            },
                        },
                    }),

                    prepareSuite(FooterAbsenceSuite, {
                        pageObjects: {
                            footer() {
                                return this.createPageObject(Footer);
                            },
                        },
                    })
                ),
            }),

            makeSuite('Ссылка «Вопросы о товаре»', {
                environment: 'kadavr',
                story: prepareSuite(ProductCardQuestionsLinkExistsSuite, {
                    hooks: {
                        async beforeEach() {
                            const config = routes.product.videoCardPalit;
                            const product = createProduct({
                                type: 'model',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: config.hid,
                                        name: 'Видеокарты',
                                        fullName: 'Видеокарты',
                                        type: 'guru',
                                        slug: 'videokarty',
                                        isLeaf: true,
                                    },
                                ],
                                slug: 'test-product',
                            }, config.productId);
                            await this.browser.setState('report', product);
                            await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                            await this.browser.yaOpenPage('touch:product', config);
                        },
                    },
                    pageObjects: {
                        productCard() {
                            return this.createPageObject(ProductCard);
                        },
                        productCardLinks() {
                            return this.createPageObject(ProductCardLinks);
                        },
                    },
                }),
            }),

            makeSuite('Ссылка «N вопросов о товаре» для товара с вопросами', {
                environment: 'kadavr',
                story: prepareSuite(ProductCardUgcSubHeaderWithQuestionsSuite, {
                    hooks: {
                        async beforeEach() {
                            const config = routes.product.videoCardPalit;
                            const product = createProduct({
                                type: 'model',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: config.hid,
                                        name: 'Видеокарты',
                                        fullName: 'Видеокарты',
                                        type: 'guru',
                                        slug: 'videokarty',
                                        isLeaf: true,
                                    },
                                ],
                                slug: config.slug,
                            }, config.productId);
                            const schema = {
                                modelQuestions: [defaultQuestion(config.productId)],
                            };
                            await this.browser.setState('schema', schema);
                            await this.browser.setState('report', product);
                            await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                            await this.browser.yaOpenPage('touch:product', config);
                        },
                    },
                    pageObjects: {
                        productCardHeader() {
                            return this.createPageObject(ProductCardHeader);
                        },
                    },
                    params: {
                        productId: routes.product.videoCardPalit.productId,
                        slug: routes.product.videoCardPalit.slug,
                    },
                }),
            }),

            makeSuite('Ссылка «N вопросов о товаре» для товара без вопросов', {
                environment: 'kadavr',
                story: prepareSuite(ProductCardUgcSubHeaderWithoutQuestionsSuite, {
                    hooks: {
                        async beforeEach() {
                            const config = routes.product.videoCardPalit;
                            const product = createProduct({
                                type: 'model',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: config.hid,
                                        name: 'Видеокарты',
                                        fullName: 'Видеокарты',
                                        type: 'guru',
                                        slug: 'videokarty',
                                        isLeaf: true,
                                    },
                                ],
                                slug: config.slug,
                            }, config.productId);
                            await this.browser.setState('report', product);
                            await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                            await this.browser.yaOpenPage('touch:product', config);
                        },
                    },
                    pageObjects: {
                        productCardHeader() {
                            return this.createPageObject(ProductCardHeader);
                        },
                    },
                }),
            }),

            makeSuite('Блок "Лента популярных вопросов"', {
                environment: 'kadavr',
                story: mergeSuites({
                    async beforeEach() {
                        await this.browser.setState('report', productWithPicture);

                        this.setPageObjects({
                            questionForm: () => this.createPageObject(QuestionForm),
                            questionCard: () => this.createPageObject(QuestionCard),
                            productCardPopularQuestions: () => this.createPageObject(ProductCardPopularQuestions),
                            smallQuestionSnippet: () => this.createPageObject(SmallQuestionSnippet),
                        });
                    },
                },

                prepareSuite(ProductCardPopularQuestionsQuestionFormSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                modelQuestions: [DEFAULT_QUESTION],
                                users: [DEFAULT_USER],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.yaProfile('ugctest3', 'touch:product', PHONE_ROUTE_CONFIG);
                        },
                        async afterEach() {
                            await this.browser.yaLogout();
                        },
                    },
                }),

                prepareSuite(QuestionsListQuestionSnippetSuite, {
                    hooks: {
                        async beforeEach() {
                            const expectedQuestionUrl = await this.browser.yaBuildURL('touch:product-question', {
                                questionSlug: DEFAULT_QUESTION.slug,
                                productSlug: PHONE_ROUTE_CONFIG.slug,
                                questionId: DEFAULT_QUESTION.id,
                                productId: PHONE_ROUTE_CONFIG.productId,
                            });
                            const schema = {
                                modelQuestions: [DEFAULT_QUESTION],
                            };

                            this.params = {
                                ...this.params,
                                expectedQuestionUrl,
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.yaOpenPage('touch:product', PHONE_ROUTE_CONFIG);
                        },
                    },
                }),

                prepareSuite(QuestionsListNoAnswersSuite, {
                    hooks: {
                        async beforeEach() {
                            const expectedQuestionUrl = await this.browser.yaBuildURL('touch:product-question', {
                                questionSlug: DEFAULT_QUESTION.slug,
                                productSlug: PHONE_ROUTE_CONFIG.slug,
                                questionId: DEFAULT_QUESTION.id,
                                productId: PHONE_ROUTE_CONFIG.productId,
                            });

                            this.params = {
                                ...this.params,
                                expectedQuestionUrl,
                            };

                            const schema = {
                                modelQuestions: [createQuestion({
                                    productId: PHONE_ROUTE_CONFIG.productId,
                                    userUid: DEFAULT_USER.id,
                                    answersCount: 0,
                                })],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.yaOpenPage('touch:product', PHONE_ROUTE_CONFIG);
                        },
                    },
                }),

                prepareSuite(QuestionsListNoQuestionsSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                modelQuestions: [],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.yaOpenPage('touch:product', PHONE_ROUTE_CONFIG);
                        },
                    },
                }),

                prepareSuite(ShowMoreButtonLessOrEqualThenThreeSuite, {
                    hooks: {
                        async beforeEach() {
                            const schema = {
                                modelQuestions: [DEFAULT_QUESTION],
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.yaOpenPage('touch:product', PHONE_ROUTE_CONFIG);
                        },
                    },
                }),

                prepareSuite(ShowMoreButtonMoreThanThreeSuite, {
                    hooks: {
                        async beforeEach() {
                            const expectedButtonUrl = await this.browser.yaBuildURL('touch:product-questions', {
                                slug: PHONE_ROUTE_CONFIG.slug,
                                productId: PHONE_ROUTE_CONFIG.productId,
                            });
                            const schema = {
                                modelQuestions: new Array(5)
                                    .fill(null)
                                    .map(() => ({...DEFAULT_QUESTION})),
                            };

                            this.params = {
                                ...this.params,
                                expectedButtonUrl,
                            };

                            await this.browser.setState('schema', schema);
                            await this.browser.yaOpenPage('touch:product', PHONE_ROUTE_CONFIG);
                        },
                    },
                })
                ),
            }),
            makeSuite('Товар с низким рейтингом.', {
                environment: 'kadavr',
                story: prepareSuite(HighratedSimilarProductsSuite, {
                    pageObjects: {
                        highratedSimilarProducts() {
                            return this.createPageObject(HighratedSimilarProducts);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const productsCount = 3;
                            const productId = routes.product.phone.productId;
                            const lowRatedProduct = {...productWithPicture};
                            lowRatedProduct.collections.product[productId].preciseRating = 3;

                            const otherProducts = [];

                            for (let i = 0; i < productsCount; i++) {
                                otherProducts.push(createProduct({
                                    showUid: `${randomString()}_${i}`,
                                    slug: 'test-product',
                                    categories: [category],
                                    preciseRating: 5,
                                }));
                            }

                            const reportState = mergeState([
                                lowRatedProduct,
                                ...otherProducts,
                            ]);

                            await this.browser.setState('report', reportState);
                            await this.browser.yaOpenPage('touch:product', PHONE_ROUTE_CONFIG);
                        },
                    },
                    params: {
                        snippetsCount: 3,
                    },
                }),
            }),
            makeSuite('Разгруппировка по магазину', {
                environment: 'kadavr',
                story: mergeSuites(
                    prepareSuite(MoreOffersDrawer, {
                        pageObjects: {
                            productOffers() {
                                return this.createPageObject(ProductOffers);
                            },
                            offerSnippet() {
                                return this.createPageObject(OfferSnippet, {
                                    parent: this.productOffers,
                                });
                            },
                            offerModifications() {
                                return this.createPageObject(OfferModifications);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                const state = buildProductOffersResultsState();
                                await this.browser.setState('report', state);

                                return this.browser.yaOpenPage('touch:product', PRODUCT_ROUTE);
                            },
                        },
                        params: {
                            shopId: SHOP_INFO.id,
                            slug: SHOP_INFO.slug,
                        },
                    })
                ),
            }),

            makeSuite('Тематики', {
                environment: 'kadavr',
                story: mergeSuites(
                    prepareSuite(SimilarTopicsSuite, {
                        pageObjects: {
                            categorySnippet() {
                                return this.createPageObject(CategorySnippet);
                            },
                            productCardLinks() {
                                return this.createPageObject(ProductCardLinks);
                            },
                            productPage() {
                                return this.createPageObject(ProductPage);
                            },
                            widgetWrapper() {
                                return this.createPageObject(WidgetWrapper, {
                                    parent: '[data-zone-name="CategorySnippet"]',
                                });
                            },
                            searchHeaderRedesigned() {
                                return this.createPageObject(SearchHeaderRedesigned);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', productStateWithFormula);
                                await this.browser.setState('Cataloger.tree', formulaCatalogerMock);

                                await this.browser.yaOpenPage('touch:product', PRODUCT_ROUTE);
                                await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                            },
                        },
                        params: {
                            expectedItemsCount: 6,
                        },
                    })
                ),
            }),

            productReviewFormMicro,
            productReviewsScrollBox,

            CartSinsTunnelingPopupSuite
        ),

        'Групповая модель.': mergeSuites(
            prepareSuite(BaseSuite, {
                hooks: {
                    beforeEach() {
                        const queryParams = routes.product.mattress;
                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product', queryParams)
                            .then(() => {
                                this.params = assign(
                                    {
                                        expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                        `${queryParams.slug}/${queryParams.productId}`,
                                        expectedOpenGraphDescriptionRegex: '^Подробные характеристики' +
                                            ' Матрас Аскона Victory,' +
                                            ' отзывы покупателей, обзоры и обсуждение товара на форуме\\.' +
                                            ' Выбирайте из более \\d+ предложени[йея] в проверенных магазинах\\.$',
                                        expectedOpenGraphTitle: 'Матрас Аскона Victory — Матрасы' +
                                            ' — купить по выгодной цене на Яндекс.Маркете',
                                    },
                                    this.params
                                );
                            });
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),

            prepareSuite(ProductCardSuite, {
                meta: {
                    environment: 'kadavr',
                },
                hooks: {
                    async beforeEach() {
                        const title = 'Групповая модель';
                        const product = createProductMock(1, 'group', title);

                        await this.browser.setState('report', product);
                        await this.browser.yaOpenPage('touch:product', {
                            productId: 1,
                            slug: 'product',
                        });

                        this.params = assign(
                            {expectedTitle: title},
                            this.params
                        );
                    },
                },
                pageObjects: {
                    productCard() {
                        return this.createPageObject(ProductCard);
                    },
                    productCardHeader() {
                        return this.createPageObject(ProductCardHeader);
                    },
                },
            })
        ),

        'Гуру-модель.': mergeSuites(
            prepareSuite(BaseSuite, {
                meta: {
                    environment: 'kadavr',
                },
                hooks: {
                    async beforeEach() {
                        const queryParams = routes.product.phone;
                        await this.browser.setState('report', productWithPicture);
                        await this.browser.yaSimulateBot();
                        await this.browser.yaOpenPage('touch:product', queryParams);

                        this.params = assign(
                            {
                                expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                        `${queryParams.slug}/${queryParams.productId}`,
                                expectedOpenGraphDescriptionRegex: '^Подробные характеристики' +
                                        ' Тестовый телефон,' +
                                        ' отзывы покупателей, обзоры и обсуждение товара на форуме\\.' +
                                        '( Выбирайте из более \\d+ предложени[йея]' +
                                        ' в проверенных магазинах\\.)?$',
                                expectedOpenGraphTitle: 'Тестовый телефон — Мобильные телефоны' +
                                    ' — купить по выгодной цене на Яндекс.Маркете',
                            },
                            this.params
                        );
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),


            prepareSuite(ProductCardSuite, {
                meta: {
                    environment: 'kadavr',
                },
                hooks: {
                    async beforeEach() {
                        const title = 'Гуру модель';
                        const product = createProductMock(1, 'guru', title);

                        await this.browser.setState('report', product);
                        await this.browser.yaOpenPage('touch:product', {
                            productId: 1,
                            slug: 'product',
                        });

                        this.params = assign(
                            {expectedTitle: title},
                            this.params
                        );
                    },
                },
                pageObjects: {
                    productCard() {
                        return this.createPageObject(ProductCard);
                    },
                    productCardHeader() {
                        return this.createPageObject(ProductCardHeader);
                    },
                },
            }),

            prepareSuite(ProductOffersSortSuite, {
                meta: {
                    environment: 'kadavr',
                },
                pageObjects: {
                    productOffers() {
                        return this.createPageObject(ProductOffers);
                    },
                },
                params: {
                    offersCount: 2,
                    title: 'Гуру модель',
                    query: {
                        productId: 1,
                        slug: 'product',
                    },
                },
                hooks: {
                    async beforeEach() {
                        const {offersCount, query, title} = this.params;
                        const product = createProductMock(query.productId, 'guru', title);
                        const state = mergeReportState([product, {
                            data: {
                                search: {
                                    total: offersCount,
                                    totalOffers: offersCount,
                                    totalOffersBeforeFilters: offersCount,
                                    totalModels: 0,
                                },
                            },
                        }]);

                        await this.browser
                            .setState('report', state)
                            .yaOpenPage('touch:product-offers', query)
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
            })
        ),

        'Кластер.': mergeSuites(
            prepareSuite(BaseSuite, {
                meta: {
                    environment: 'kadavr',
                },
                hooks: {
                    async beforeEach() {
                        const queryParams = routes.product.phone;
                        await this.browser.setState('report', productClusterWithPicture);
                        await this.browser.yaSimulateBot();
                        await this.browser.yaOpenPage('touch:product', queryParams);

                        this.params = assign(
                            {
                                expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                    `${queryParams.slug}/${queryParams.productId}`,
                                expectedOpenGraphDescription: 'Тестовый телефон. Цены, отзывы покупателей' +
                                    ' о товаре и магазинах, условия доставки и возврата — всё на одной' +
                                    ' странице.',
                                expectedOpenGraphTitle: 'Тестовый телефон — Мобильные телефоны' +
                                    ' — купить по выгодной цене на Яндекс.Маркете',
                            },
                            this.params
                        );
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),

            prepareSuite(ProductCardSuite, {
                meta: {
                    environment: 'kadavr',
                },
                hooks: {
                    async beforeEach() {
                        const title = 'Кластер модель';
                        const product = createProductMock(1, 'cluster', title);

                        await this.browser.setState('report', product);
                        await this.browser.yaOpenPage('touch:product', {
                            productId: 1,
                            slug: 'product',
                        });

                        this.params = assign(
                            {expectedTitle: title},
                            this.params
                        );
                    },
                },
                pageObjects: {
                    productCard() {
                        return this.createPageObject(ProductCard);
                    },
                    productCardHeader() {
                        return this.createPageObject(ProductCardHeader);
                    },
                },
            }),

            makeSuite('Ссылка «Вопросы о товаре»', {
                environment: 'kadavr',
                story: prepareSuite(ProductCardQuestionsLinkExistsSuite, {
                    hooks: {
                        async beforeEach() {
                            const config = routes.product.videoCardPalit;
                            const product = createProduct({
                                type: 'cluster',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: config.hid,
                                        name: 'Видеокарты',
                                        fullName: 'Видеокарты',
                                        type: 'guru',
                                        slug: 'videokarty',
                                        isLeaf: true,
                                    },
                                ],
                            }, String(config.productId));
                            await this.browser.setState('report', product);
                            await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                            await this.browser.yaOpenPage('touch:product', config);
                        },
                    },
                    pageObjects: {
                        productCard() {
                            return this.createPageObject(ProductCard);
                        },
                        productCardLinks() {
                            return this.createPageObject(ProductCardLinks);
                        },
                    },
                }),
            })
        ),

        'Книга.': mergeSuites(
            prepareSuite(BaseSuite, {
                params: {
                    // MOBMARKET-10116: skip падающих тестов на opengraph:image,
                    // тикет на починку MOBMARKET-9726
                    skipOpenGraphImage: true,
                },
                hooks: {
                    beforeEach() {
                        const queryParams = routes.product.book;
                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product', queryParams)
                            .then(() => {
                                this.params = assign(
                                    {
                                        expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                            `${queryParams.slug}/${queryParams.productId}`,
                                        expectedOpenGraphDescriptionRegex: '^Подробные характеристики' +
                                            ' Пауло Коэльо "Алхимик",' +
                                            ' отзывы покупателей, обзоры и обсуждение товара на форуме\\.' +
                                            '( Выбирайте из более \\d+ предложени[йея]' +
                                            ' в проверенных магазинах\\.)?$',
                                        expectedOpenGraphTitle: 'Пауло Коэльо "Алхимик" — Зарубежная проза и поэзия' +
                                            ' — купить по выгодной цене на Яндекс.Маркете',
                                    },
                                    this.params
                                );
                            });
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
            }),

            prepareSuite(ProductCardSuite, {
                meta: {
                    environment: 'kadavr',
                },
                hooks: {
                    async beforeEach() {
                        const title = 'Книга модель';
                        const product = createProductMock(1, 'book', title);

                        await this.browser.setState('report', product);
                        await this.browser.yaOpenPage('touch:product', {
                            productId: 1,
                            slug: 'product',
                        });

                        this.params = assign(
                            {expectedTitle: title},
                            this.params
                        );
                    },
                },
                pageObjects: {
                    productCard() {
                        return this.createPageObject(ProductCard);
                    },
                    productCardHeader() {
                        return this.createPageObject(ProductCardHeader);
                    },
                },
            }),

            makeSuite('Ссылка «Вопросы о товаре»', {
                environment: 'kadavr',
                story: prepareSuite(ProductCardQuestionsLinkExistsSuite, {
                    hooks: {
                        async beforeEach() {
                            const config = routes.product.videoCardPalit;
                            const product = createProduct({
                                type: 'book',
                                categories: [
                                    {
                                        entity: 'category',
                                        id: config.hid,
                                        name: 'Видеокарты',
                                        fullName: 'Видеокарты',
                                        type: 'guru',
                                        slug: 'videokarty',
                                        isLeaf: true,
                                    },
                                ],
                            }, String(config.productId));
                            await this.browser.setState('report', product);
                            await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                            await this.browser.yaOpenPage('touch:product', config);
                        },
                    },
                    pageObjects: {
                        productCard() {
                            return this.createPageObject(ProductCard);
                        },
                        productCardLinks() {
                            return this.createPageObject(ProductCardLinks);
                        },
                    },
                }),
            })
        ),

        'Футер страницы.': createStories(
            seoTestConfigs.desktopLink,
            ({routeParams}) => prepareSuite(FooterSuite, {
                pageObjects: {
                    footer() {
                        return this.createPageObject(Footer);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const state = mergeReportState([
                            createProduct({}, routeParams.productId),
                        ]);

                        await this.browser.setState('report', state);
                        return this.browser.yaOpenPage('touch:product', routeParams);
                    },
                },
            })
        ),

        'SEO-разметка страницы.': createStories(
            seoTestConfigs.pageCanonical,
            ({routeConfig, testParams, description}) => prepareSuite(BaseLinkCanonicalSuite, {
                hooks: {
                    beforeEach() {
                        if (description === 'Тип "Визуальная"') {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip(
                                'MOBMARKET-10116: Скипаем падающие автотесты, ' +
                                'тикет на починку MOBMARKET-9726'
                            );
                        }

                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product', routeConfig);
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
                params: testParams.main,
            })
        ),
        'Предзаказ.': preorder,
        'Подписки.': subscription,
        ageConfirmation,
        vendorLine,
        automaticallyCalculatedDelivery,
        'Акции.': deals,
        upsale,
        promoBadge,
        gallery,
        credit,
        bnplSuite,
        'Быстрые фильтры.': embeddedFilters,
        'Страница всех фильтров.': allFiltersPage,
        'Фильтры.': filters,
        'Блок с информацией о продавце. Товар в продаже': linkSellersInfo,
        'Блок с информацией о продавце. Товар не в продаже.': linkSellersInfoNotInStock,
        'Лекарственный дисклеймер.': drugsDisclaimer,
        'Почта России': russianPost,
        'Доставка в ДО.': defaultOfferDelivery,
        'Виджет UGC Медиа галереи.': prepareSuite(UgcMediaGallerySuite, {
            hooks: {
                async beforeEach() {
                    const testProductId = 1722193751;
                    const testSlug = 'test-slug';
                    const reviewUser = profiles.ugctest3.uid;
                    const testReview = createUserReviewWithPhotos(testProductId, reviewUser);
                    const testProduct = createProduct({
                        type: 'model',
                        categories: [{
                            id: 123,
                        }],
                        slug: testSlug,
                        deletedId: null,
                        prices: createPriceRange(300, 400, 'RUB'),
                    }, testProductId);

                    await this.browser
                        .setState('report', testProduct)
                        .setState('schema', {
                            gradesOpinions: [testReview],
                            modelOpinions: [testReview],
                        });

                    const testUser = profiles['pan-topinambur'];
                    await this.browser.yaLogin(
                        testUser.login,
                        testUser.password
                    );

                    await this.browser.yaOpenPage('touch:product', {
                        productId: testProductId,
                        slug: testSlug,
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                    await this.browser.yaSlowlyScroll(GalleryUgcSlider.root);
                },
            },
            pageObjects: {
                galleryUgcSlider() {
                    return this.createPageObject(GalleryUgcSlider);
                },
            },
            params: {
                productId: 1722193751,
                slug: 'test-slug',
            },
        }),
        'Деградация': prepareSuite(ProductDegradationSuite, {

            pageObjects: {
                productCard() {
                    return this.createPageObject(ProductCard);
                },
                productCardHeader() {
                    return this.createPageObject(ProductCardHeader);
                },
            },
            hooks: {
                async beforeEach() {
                    const config = routes.product.videoCardPalit;

                    const videocardProductOptions = {
                        type: 'model',
                        categories: [
                            {
                                entity: 'category',
                                id: config.hid,
                                name: 'Видеокарты',
                                fullName: 'Видеокарты',
                                type: 'guru',
                                slug: 'videokarty',
                                isLeaf: true,
                            },
                        ],
                        vendor: {
                            name: 'Palit',
                            filter: '7893318:779586',
                        },
                    };
                    const reportState = createProduct(videocardProductOptions, String(config.productId));
                    await this.browser.setState('report', reportState);
                    await this.browser.setState('Cataloger.tree', breadcrumbsCatalogerMock);

                    await this.browser.yaOpenPage(
                        'touch:product',
                        Object.assign({}, config, {sku: 1111})
                    );
                },
            },
        }),
        placementTypes,
        'Модель с экспресс-доставкой.': expressDefaultOffer,
        'Модель со вторым ДО.': secondaryExpressOffer,

        'Выгода Плюса.': prepareSuite(DeliveryBetterWithPlus, {
            params: {
                pageId: 'touch:product',
            },
            pageObjects: {
                freeDeliveryWithPlusLink() {
                    return this.createPageObject(FreeDeliveryWithPlusLink);
                },
            },
        }),
        'Баннер после обратного скролла.': prepareSuite(bannerAfterReverseScroll, {
            params: {
                prepareState: async function () {
                    const dataMixin = {
                        data: {
                            search: {
                                total: 1,
                                totalOffers: 1,
                            },
                        },
                    };

                    await this.browser.setState('report', mergeState([
                        productWithDefaultOffer,
                        dataMixin,
                    ]));

                    await this.browser.yaSetCookie({
                        name: 'currentRegionId',
                        value: '213',
                    });

                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
        }),
        defaultOffer,
        topOffers,
        CartPopupCounterUnitsSuite,
        virtualPackSuite,
        'Цифровой продукт': prepareSuite(digitalOfferSuite),
        'Продукт на заказ': prepareSuite(uniqueOfferSuite),
        'Продукт с неточной датой доставки': prepareSuite(estimatedOfferSuite),
    },
});
