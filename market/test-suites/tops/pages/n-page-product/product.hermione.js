import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import _ from 'lodash';

import {
    createProduct,
    createOffer,
    mergeState,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';
// import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import {createSurveyFormMock} from '@self/project/src/spec/hermione/helpers/metakadavr';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
import Model from '@self/platform/spec/hermione/configs/test-models';
import {reviewWithComment, reviewWithCommentAndPhotos} from '@self/platform/spec/hermione/configs/reviews/mocks';
import {shopFeedbackFormId} from '@self/platform/spec/hermione/configs/forms';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import virtualPack from '@self/platform/spec/hermione/test-suites/blocks/virtualPacks/';
import FilterPickerDO from '@self/platform/spec/hermione/test-suites/blocks/ColorFilter/coloredDO';
import FilterPickerTopOffers from '@self/platform/spec/hermione/test-suites/blocks/ColorFilter/coloredTopOffers';
import FilterPickerColoredGallery from '@self/platform/spec/hermione/test-suites/blocks/ColorFilter/coloredGallery';
import RatingSuite from '@self/platform/spec/hermione/test-suites/blocks/rating';
import ProductSummarySuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSummary';
import GalleryVideoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-gallery/videoContent';
import ShopInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info';
import ShopInfoNotInStockSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/not-in-stock';
import AlsoViewedProductsSuite from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/alsoViewedProducts';
import ScrollBoxWidgetTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBoxWidget/title-text';
import ProductSpecsSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSpecs';
import ProductTopOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-top-offer';
import PopupComplainSuite from '@self/platform/spec/hermione/test-suites/blocks/PopupComplain/shopAndOffer';
import ProductDefaultOfferValidOfferIdSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-default-offer/valid-offer-id';
import ProductDegradationSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-degradation';
// import ProductTabsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-tabs';
import DeliveryBetterWithPlus from '@self/project/src/spec/hermione/test-suites/blocks/DefaultOffer/deliveryBetterWithPlus';
import ProductTitleWithQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-title/withQuestions';
import ProductTitleWithoutQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-title/withoutQuestions';
import SkuProductTitle from '@self/platform/spec/hermione/test-suites/blocks/n-product-title/sku';
import SkuProductLinks from '@self/platform/spec/hermione/test-suites/blocks/n-product-title/links';
import SkuProductURL from '@self/platform/spec/hermione/test-suites/blocks/n-product-url/sku';
import SkuProductFilters from '@self/platform/spec/hermione/test-suites/blocks/n-product-filters/sku';
import ProductBindedFilters from '@self/platform/spec/hermione/test-suites/blocks/n-product-filters/binded';
import HeadBannerProductAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/productAbsence';
import AdultWarningDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/default';
import AdultWarningAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/accept';
import AdultWarningDeclineSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/decline';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import UgcMediaGallerySuite from '@self/platform/spec/hermione/test-suites/blocks/UgcMediaGallery';
import UgcMediaGalleryVideoSuite from '@self/platform/spec/hermione/test-suites/blocks/UgcMediaGallery/video';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import TopOfferFull from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/topOfferFull';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import GenericBundleTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import PromoFlashTermSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoFlash';
import DsbsCompactOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-top-offer/dsbsCompactOffer';
import CartUpsalePopupSuite from '@self/platform/spec/hermione/test-suites/blocks/CartUpsalePopup';
import DirectDiscountPromo from '@self/root/src/spec/hermione/test-suites/blocks/DirectDiscountPromo';
import UnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc';
import CartPopupUnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/cartPopup';

// page-objects
import TopOffers from '@self/platform/spec/page-objects/widgets/content/TopOffers';
import MiniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import DefaultOfferTitle from '@self/platform/components/DefaultOfferTitle/__pageObject';
import OfferSpecsItem from '@self/platform/components/SpecsFromFilters/Spec/__pageObject';
import Region from '@self/platform/spec/page-objects/region';
import ProductSummary from '@self/platform/spec/page-objects/ProductSummary';
import ProductHeadnote from '@self/platform/spec/page-objects/n-product-headnote';
import ColorFilterPageObject from '@self/platform/components/ColorFilter/__pageObject__';
import RatingBadge from '@self/project/src/components/RatingBadge/__pageObject/index.desktop';
import LegalInfo from '@self/root/src/components/LegalInfo/__pageObject';
import ImageGallery from '@self/platform/components/ImageGallery/__pageObject';
import ImageGalleryPopup from '@self/platform/components/ImageGallery/GalleryModal/__pageObject';
import VideoGalleryContent from '@self/platform/components/videoGallery/videoStream/__pageObject';
import ShopName from '@self/project/src/components/ShopName/__pageObject';
import ScrollBoxWidget from '@self/platform/spec/page-objects/ScrollBoxWidget';
import ProductSpecsPageObject from '@self/platform/components/ProductSpecs/__pageObject__';
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import PopupComplainForm from '@self/platform/spec/page-objects/components/ComplainPopup';
import ComplainButton from '@self/platform/spec/page-objects/components/ComplainButton';
import Preloadable from '@self/platform/spec/page-objects/preloadable';
import OfferPaymentTypes from '@self/platform/spec/page-objects/components/OfferPaymentTypes';
// import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import ProductTitle from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';
import UgcMediaGallery from '@self/platform/widgets/content/UgcMediaGallery/__pageObject';
import UgcGalleryFloatingReview from '@self/root/src/components/UgcGalleryFloatingReview/__pageObject/index';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import TopOfferSnippetCompact from '@self/platform/spec/page-objects/components/TopOfferSnippetCompact';
import TopOfferActions from '@self/platform/components/TopOfferSnippet/components/TopOfferActions/__pageObject';
import PromoBadge from '@self/root/src/components/PromoBadge/__pageObject';
import HintTooltip from '@self/project/src/components/DealsTerms/HintTooltip/__pageObject';
import OfferPrice from '@self/platform/spec/page-objects/components/OfferPrice';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import RecommendedOffers from '@self/platform/widgets/content/RecommendedOffers/__pageObject';
import OfferSet from '@self/project/src/components/OfferSet/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import PromoFlashDescription from '@self/project/src/components/BlueFlashDescription/__pageObject';
import TimerFlashSale from '@self/project/src/components/TimerFlashSale/__pageObject';
import DirectDiscountTerms from '@self/root/src/components/DirectDiscountTerms/__pageObject';
import FreeDeliveryWithPlusLink from '@self/root/src/components/FreeDeliveryWithPlusLink/__pageObject';
import MorePricesLink from '@self/platform/widgets/content/MorePricesLink/__pageObject';
import UnitsCalc from '@self/root/src/components/UnitsCalc/__pageObject';
import AmountSelect from '@self/project/src/components/AmountSelect/__pageObject';

// mocks
import {groupMock, bookMock, guruMock, clusterMock} from '@self/platform/spec/hermione/fixtures/promo/product.mock';
import {offerMock} from '@self/platform/spec/hermione/fixtures/promo/offer.mock';
import {prepareKadavrReportStateWithDefaultState} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import {prepareKadavrReportState} from '@self/project/src/spec/hermione/fixtures/promo/flash';
import {spreadDiscountCountPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';

// fixtures
import oneOfferCpa
    from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/oneOfferCPA';
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import {unitInfo} from '@self/platform/spec/hermione/fixtures/unitInfo';
import {DEFAULT_VIDEO_ID, createUserProductVideos} from '@self/platform/spec/hermione/fixtures/ugcvideo';
import {
    prepareKadavrReportStateForDirectDiscount,
    promoText as directDiscountText,
} from '@self/project/src/spec/hermione/fixtures/promo/directDiscount';
import {phone, notebook} from './fixtures/product';
import productWithTop6Offer from './fixtures/productWithTop6Offer';
import productWithAlsoViewed from './fixtures/productWithAlsoViewed';
import productWithTop6OfferCPA from './fixtures/productWithTop6OfferCPA';
import productWithTop6OfferDSBS from './fixtures/productWithTop6OfferDSBS';
import productWithColoredPicturesFixture from './fixtures/productWithColoredPictures';
import productWithSkuFiltersFixture from './fixtures/productWithSkuFilters';
import productWithExternalLinkInDO from './fixtures/productWithExternalLinkInDO';
import productWithWareMd5Offer from './fixtures/productWithWareMd5Offer';
import * as productWithVideoGallery from './fixtures/productWithVideoGallery';
import productWithCashbackInDO, {
    CASHBACK_AMOUNT as CASHBACK_AMOUNT_DO,
} from './fixtures/productWithCashbackInDO';
import productWithExtraCashbackInDO from './fixtures/productWithExtraCashbackInDO';
import productWithTop6CashbackOffer, {
    CASHBACK_AMOUNT as CASHBACK_AMOUNT_TOP6,
} from './fixtures/productWithTop6CashbackOffer';
import productWithTop6ExtraCashbackOffer from './fixtures/productWithTop6ExtraCashbackOffer';

import fixturesForColor from './fixtures/fixturesForColorTest';
// imports
import credit from './credit';
import pickup from './pickup';
import whitePreorder from './whitePreorder';
import preorder from './preorder';
import metrika from './metrika';
import seo from './seo';
// import shopLogoDefaultOffer from './shopLogo/defaultOffer';
import shopLogoTop6 from './shopLogo/top6';
import paOnSaleSubscription from './paOnSaleSubscription';
import ageConfirmation from './ageConfirmation';
import placementTypesDefaultOffer from './placementTypes/defaultOffer';
import automticallyCalculatedDelivery from './automticallyCalculatedDelivery';
import promo from './promo';
import setBlockData from './promo/setBlockData';
import productWithBlueSetInDO from './fixtures/productWithBlueSetInDO';
import recommendedOffers from './recommendedOffers';
import secondaryExpressOffer from './express/secondaryExpressOffer';
import {bnplSuite} from './bnpl';
import vendorLine from './vendorLine';
import unitInfoDefaultOfferSuite from './unitInfo/defaultOffer';
import unitInfoTop6Suite from './unitInfo/top6';
import unitInfoCartPopup from './unitInfo/cartPopup';

const coloredPicturesMock = productWithColoredPicturesFixture.picturesMock;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница карточки модели.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Фильтры по цвету с SKU.', {
            environment: 'kadavr',
            story: mergeSuites(
                makeSuite('Изменение фильтра.', {
                    story: prepareSuite(FilterPickerColoredGallery, {
                        pageObjects: {
                            colorFilter() {
                                return this.createPageObject(ColorFilterPageObject, {
                                    root: ProductSummary.root,
                                });
                            },
                            gallery() {
                                return this.createPageObject(ImageGallery);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', productWithSkuFiltersFixture.initState);
                                return this.browser.yaOpenPage('market:product', productWithSkuFiltersFixture.route);
                            },
                        },
                        params: {
                            skuState: productWithSkuFiltersFixture.skuState,
                            expectedMainPicture: productWithSkuFiltersFixture.skuPicture,
                            expectedThumbs: productWithSkuFiltersFixture.skuThumbs,
                            selectedPickerIndex: 2,
                            expectedResetButtonVisible: false,
                        },
                    }),
                }),
                makeSuite('Изменения ДО.', {
                    feature: 'Пикер цвета обновляет данные',
                    story: prepareSuite(FilterPickerDO, {
                        pageObjects: {
                            offerSpecsItem() {
                                return this.createPageObject(OfferSpecsItem, {
                                    parent: ProductDefaultOffer.root,
                                });
                            },
                            defaultOffer() {
                                return this.createPageObject(ProductDefaultOffer);
                            },
                            colorFilter() {
                                return this.createPageObject(ColorFilterPageObject);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', productWithSkuFiltersFixture.initState);
                                return this.browser.yaOpenPage('market:product', productWithSkuFiltersFixture.route);
                            },
                        },
                        params: {
                            skuState: productWithSkuFiltersFixture.skuState,
                            expectedOfferId: '2001',
                        },
                    }),
                }),
                makeSuite('Изменения заголовка модели', {
                    story: prepareSuite(SkuProductTitle, {
                        params: {
                            selectedPickerIndex: 2,
                            expectedTitle: 'Смартфон Samsung Galaxy S8, желтый',
                            skuState: productWithSkuFiltersFixture.skuState,
                        },
                        pageObjects: {
                            colorFilter() {
                                return this.createPageObject(ColorFilterPageObject, {
                                    root: ProductSummary.root,
                                });
                            },

                            productTitle() {
                                return this.createPageObject(ProductTitle);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', productWithSkuFiltersFixture.initState);
                                return this.browser.yaOpenPage('market:product', productWithSkuFiltersFixture.route);
                            },
                        },
                    }),
                }),
                makeSuite('Изменение параметров в url', {
                    story: prepareSuite(SkuProductURL, {
                        params: {
                            selectedPickerIndex: 2,
                            skuState: productWithSkuFiltersFixture.skuState,
                        },
                        pageObjects: {
                            colorFilter() {
                                return this.createPageObject(ColorFilterPageObject, {
                                    root: ProductSummary.root,
                                });
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', productWithSkuFiltersFixture.initState);
                                return this.browser.yaOpenPage('market:product', productWithSkuFiltersFixture.route);
                            },
                        },
                    }),
                }),
                prepareSuite(SkuProductFilters, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithSkuFiltersFixture.initState);
                            return this.browser.yaOpenPage('market:product', productWithSkuFiltersFixture.route);
                        },

                    },
                    pageObjects: {
                        colorFilter() {
                            return this.createPageObject(ColorFilterPageObject, {
                                root: ProductSummary.root,
                            });
                        },
                        recommendedOffers() {
                            return this.createPageObject(RecommendedOffers);
                        },
                    },
                    params: {
                        selectedPickerIndex: 2,
                        skuId: productWithSkuFiltersFixture.skuYellowId,
                        skuState: productWithSkuFiltersFixture.skuState,
                    },
                })
            ),
        }),
        makeSuite('SKU модель', {
            story: mergeSuites(
                makeSuite('Переход по ссылкам в шапке', {
                    environment: 'kadavr',
                    story: (() => createStories([
                        {linkElementName: 'reviewsCount', description: 'Отзывы'},
                        {linkElementName: 'specLink', description: 'Характеристики'},
                        /*
                        // Прячем до лучших времён см.MARKETFRONT-76293
                        {linkElementName: 'overviewsLink', description: 'Обзоры'},
                        */
                        {linkElementName: 'qaEntrypoint', description: 'Вопросы и ответы'},
                        {description: 'Цены', isPricesPage: true},
                    ], ({linkElementName, isPricesPage}) => prepareSuite(SkuProductLinks, {
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState('report', productWithSkuFiltersFixture.skuState);
                                return this.browser.yaOpenPage('market:product', {
                                    ...productWithSkuFiltersFixture.route,
                                    sku: productWithSkuFiltersFixture.skuYellowId,
                                });
                            },

                        },
                        params: {
                            linkElementName,
                            skuId: productWithSkuFiltersFixture.skuYellowId,
                            clickMethod: isPricesPage ? 'morePricesLinkClick' : 'click',
                        },
                        pageObjects: {
                            linkPageObject() {
                                const pageObject = this.createPageObject(isPricesPage ? MorePricesLink : ProductTitle);

                                return linkElementName ? pageObject[linkElementName] : pageObject;
                            },
                        },

                    })
                    ))(),
                }),
                prepareSuite(SkuProductFilters, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithSkuFiltersFixture.skuState);
                            return this.browser.yaOpenPage('market:product', {
                                ...productWithSkuFiltersFixture.route, sku: productWithSkuFiltersFixture.skuYellowId,
                            });
                        },
                    },
                    pageObjects: {
                        colorFilter() {
                            return this.createPageObject(ColorFilterPageObject, {
                                root: ProductSummary.root,
                            });
                        },
                        recommendedOffers() {
                            return this.createPageObject(RecommendedOffers);
                        },
                    },
                    params: {
                        selectedPickerIndex: 1,
                        skuId: productWithSkuFiltersFixture.skuBlueId,
                    },
                }),
                prepareSuite(ProductBindedFilters, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithSkuFiltersFixture.initState);
                            return this.browser.yaOpenPage('market:product', productWithSkuFiltersFixture.route);
                        },
                    },
                    pageObjects: {
                        colorFilter() {
                            return this.createPageObject(ColorFilterPageObject, {
                                root: ProductSummary.root,
                            });
                        },
                        recommendedOffers() {
                            return this.createPageObject(RecommendedOffers);
                        },
                    },
                    params: {
                        selectedPickerIndex: 2,
                        skuId: productWithSkuFiltersFixture.skuYellowId,
                        skuState: productWithSkuFiltersFixture.skuState,
                    },
                })

            ),
        }),
        makeSuite('Фильтры по цвету. Изменения ДО.', {
            environment: 'kadavr',
            feature: 'Пикер цвета обновляет данные',
            story: prepareSuite(FilterPickerDO, {
                pageObjects: {
                    offerSpecsItem() {
                        return this.createPageObject(OfferSpecsItem, {
                            parent: ProductDefaultOffer.root,
                        });
                    },
                    defaultOffer() {
                        return this.createPageObject(ProductDefaultOffer);
                    },
                    colorFilter() {
                        return this.createPageObject(ColorFilterPageObject);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const {recommendedOffersState, navState, productId, slug, filterId, goldId} = fixturesForColor;
                        await this.browser.setState('report', recommendedOffersState);
                        await this.browser.setState('Cataloger.tree', navState);

                        return this.browser.yaOpenPage('market:product', {
                            productId,
                            slug,
                            glfilter: `${filterId}:${goldId}`,
                        });
                    },
                },
                params: {
                    expectedOfferId: '101',
                },
            }),
        }),
        makeSuite('Фильтры по цвету. Изменение ТОП6.', {
            environment: 'kadavr',
            feature: 'Пикер цвета обновляет данные',
            story: prepareSuite(FilterPickerTopOffers, {
                pageObjects: {
                    topOffer() {
                        return this.createPageObject(MiniTopOffers);
                    },
                    colorFilter() {
                        return this.createPageObject(ColorFilterPageObject);
                    },
                    preloadable() {
                        return this.createPageObject(Preloadable, {parent: this.productSummary});
                    },
                },
                hooks: {
                    async beforeEach() {
                        const {topOffersState, navState, productId, slug, filterId, goldId} = fixturesForColor;

                        await this.browser.setState('report', topOffersState);
                        await this.browser.setState('Cataloger.tree', navState);

                        return this.browser.yaOpenPage('market:product', {
                            productId,
                            slug,
                            glfilter: `${filterId}:${goldId}`,
                        });
                    },
                },
            }),
        }),

        makeSuite('Фильтры по цвету. Изменение фильтра.', {
            environment: 'kadavr',
            feature: 'Пикер цвета обновляет данные',
            story: prepareSuite(FilterPickerColoredGallery, {
                pageObjects: {
                    colorFilter() {
                        return this.createPageObject(ColorFilterPageObject, {
                            root: ProductSummary.root,
                        });
                    },
                    gallery() {
                        return this.createPageObject(ImageGallery);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', productWithColoredPicturesFixture.state);
                        return this.browser.yaOpenPage('market:product', productWithColoredPicturesFixture.route);
                    },
                },
                params: {
                    expectedMainPicture: `https:${coloredPicturesMock[21].original.url}`,
                    expectedThumbs: [
                        `https:${coloredPicturesMock[21].thumbnails[0].url}`,
                        `https:${coloredPicturesMock[22].thumbnails[0].url}`,
                        `https:${coloredPicturesMock[23].thumbnails[0].url}`,
                        `https:${coloredPicturesMock[24].thumbnails[0].url}`,
                        `https:${coloredPicturesMock[25].thumbnails[0].url}`,
                    ],
                    selectedPickerIndex: 3,
                },
            }),
        }),

        makeSuite('Рейтинг.', {
            environment: 'testing',
            id: 'marketfront-43',
            feature: 'Визитка',
            story: mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            productSummary: () => this.createPageObject(ProductSummary),
                            productHeadnote: () => this.createPageObject(ProductHeadnote),
                            rating: () => this.createPageObject(RatingBadge, {parent: ProductTitle.root}),
                        });

                        return this.browser.yaOpenPage('market:product', routes.product.withRating);
                    },
                },

                prepareSuite(RatingSuite),
                prepareSuite(ProductSummarySuite)
            ),
        }),

        makeSuite('Галерея.', {
            environment: 'kadavr',
            story: mergeSuites(
                prepareSuite(GalleryVideoSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithVideoGallery.state);
                            await this.browser.setState('S3Mds.files', {
                                '/products_video/links.json': [productWithVideoGallery.s3MdsFixture],
                            });
                            await this.browser.setState('vhFrontend', productWithVideoGallery.vhFrontendFixture);
                            return this.browser.yaOpenPage('market:product', productWithVideoGallery.route);
                        },
                    },
                    pageObjects: {
                        gallery() {
                            return this.createPageObject(ImageGallery);
                        },
                        galleryPopup() {
                            return this.createPageObject(ImageGalleryPopup);
                        },
                        galleryVideoContent() {
                            return this.createPageObject(VideoGalleryContent);
                        },
                    },
                    params: {
                        thumbsCount: 4,
                        defaultActiveThumb: 1,
                        galleryImagesCount: 8,
                    },
                })
            ),
        }),

        makeSuite('Модель в продаже.', {
            issue: 'MARKETVERSTKA-26681',
            environment: 'kadavr',
            story: (() => createStories(
                [
                    groupMock,
                    bookMock,
                    guruMock,
                    clusterMock,
                ],
                ({mock}) => prepareSuite(ShopInfoSuite, {
                    hooks: {
                        async beforeEach() {
                            const product = createProduct(mock, mock.id);
                            const offer = createOffer(_.assign({
                                benefit: {
                                    type: 'recommended',
                                    description: 'Хорошая цена от надёжного магазина',
                                    isPrimary: true,
                                },
                            }, offerMock), offerMock.wareId);

                            const state = mergeState([product, offer, {
                                data: {
                                    search: {
                                        totalOffersBeforeFilters: 2,
                                    },
                                },
                            }]);

                            await this.browser.setState('report', state);

                            await this.browser.yaOpenPage('market:product', {
                                productId: mock.id,
                                slug: mock.slug,
                            });

                            await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                        },
                    },
                    pageObjects: {
                        shopsInfo() {
                            return this.createPageObject(LegalInfo);
                        },
                        shopName() {
                            return this.createPageObject(ShopName);
                        },
                        shopsTop6Info() {
                            return this.createPageObject(TopOffers);
                        },
                    },
                })
            ))(),
        }),

        makeSuite('Модель не в продаже.', {
            issue: 'MARKETVERSTKA-26681',
            story: (() => {
                const testData = _.map(
                    Model.groupedByTypeNotInStock,
                    (value, key) => _.merge(value, {routeParams: routes.product.groupedByTypeNotInStock[key]})
                );

                return createStories(
                    testData,
                    params => prepareSuite(ShopInfoNotInStockSuite, {
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:product', params.routeParams);
                                await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                            },
                        },
                        pageObjects: {
                            shopsInfo() {
                                return this.createPageObject(LegalInfo);
                            },
                        },
                    })
                );
            })(),
        }),

        makeSuite('Модель в продаже. Блок "С этим товаром смотрят"', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            scrollBox: () => this.createPageObject(ScrollBoxWidget),
                        });

                        await this.browser.setState('report', productWithAlsoViewed.state);
                        await this.browser.yaOpenPage('market:product', productWithAlsoViewed.route);
                        await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    },
                },

                prepareSuite(AlsoViewedProductsSuite, {
                    meta: {
                        id: 'marketfront-2511',
                        issue: 'MARKETVERSTKA-29052',
                    },
                    params: {
                        title: 'С этим товаром смотрят',
                    },
                })
            ),
        }),
        makeSuite('Модель не в продаже. Блок "С этим товаром смотрят"', {
            environment: 'testing',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            scrollBoxWidget: () => this.createPageObject(
                                ScrollBoxWidget,
                                {
                                    root: '#analogues-products',
                                }
                            ),
                        });

                        await this.browser.yaOpenPage(
                            'market:product',
                            {
                                productId: 11028554,
                                slug: 'smartfon-sony-xperia-z3-compact',
                            }
                        );

                        return this.browser.scroll('#analogues-products');
                    },
                },

                prepareSuite(ScrollBoxWidgetTitleSuite, {
                    meta: {
                        id: 'marketfront-2512',
                        issue: 'MARKETVERSTKA-29052',
                    },
                    params: {
                        titleText: 'С этим товаром смотрят',
                    },
                })
            ),
        }),

        makeSuite('Колонка "Коротко о товаре".', {
            story: mergeSuites(
                createStories(
                    [
                        {
                            description: 'Групповая модель',
                            specBlockSuite: {
                                params: {
                                    productId: notebook.id,
                                    slug: notebook.slug,
                                },
                                meta: {
                                    id: 'marketfront-34',
                                    issue: 'MARKETVERSTKA-23877',
                                },
                            },
                        },
                    ],

                    ({specBlockSuite}) => mergeSuites(
                        prepareSuite(ProductSpecsSuite, {
                            hooks: {
                                async beforeEach() {
                                    const friendly = new Array(7)
                                        .fill(null)
                                        .map((item, index) => `Характеристика ${index}`);
                                    const product = Object.assign({}, notebook, {specs: {friendly}});
                                    const state = createProduct(product, notebook.id);

                                    await this.browser
                                        .setState('report', state)
                                        .yaOpenPage('market:product', this.params);
                                },
                            },
                            pageObjects: {
                                productSpecs() {
                                    return this.createPageObject(ProductSpecsPageObject, {
                                        parent: ProductSummary.root,
                                    });
                                },
                            },
                            ..._.pick(specBlockSuite, 'params', 'meta'),
                        })
                    )
                )
            ),
        }),

        makeSuite('Блок "Топ 6"', {
            environment: 'kadavr',
            story: mergeSuites(
                // TODO: вендора отрубили фильтр по гарантии
                prepareSuite(ProductTopOfferSuite, {
                    pageObjects: {
                        shopsTop6Info() {
                            return this.createPageObject(MiniTopOffers);
                        },
                        button() {
                            return this.createPageObject(ClickoutButton, {
                                parent: `${MiniTopOffers.item}:first-child`,
                            });
                        },
                        clickOutButton() {
                            return this.createPageObject(ClickoutButton, {
                                root: `${MiniTopOffers.item}:first-child`,
                            });
                        },
                        paymentType() {
                            return this.createPageObject(OfferPaymentTypes, {
                                parent: `${MiniTopOffers.item}:first-child`,
                            });
                        },
                        topOfferSnippetCompact() {
                            return this.createPageObject(TopOfferSnippetCompact, {
                                root: `${MiniTopOffers.item}:first-child`,
                            });
                        },
                    },
                    params: {
                        urlDomain: 'market-click2-testing.yandex.ru',
                        url: productWithTop6Offer.encryptedUrl,
                        paymentTypeText: 'картой на сайте',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithTop6Offer.state);
                            return this.browser.yaOpenPage('market:product', productWithTop6Offer.route);
                        },
                    },
                }),
                makeSuite('CPA-оффер в топ-6', {
                    environment: 'kadavr',
                    meta: {
                        issue: 'MARKETFRONT-13236',
                    },
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', productWithTop6OfferCPA.state);

                                return this.browser.yaOpenPage('market:product', productWithTop6OfferCPA.route);
                            },
                        },
                        prepareSuite(CartButtonSuite),
                        prepareSuite(CartButtonCounterSuite),
                        prepareSuite(ItemCounterCartButtonSuite, {
                            params: {
                                counterStep: productWithTop6OfferCPA.offerCPAMock.bundleSettings.quantityLimit.step,
                                offerId: productWithTop6OfferCPA.offerCPAMock.wareId,
                                withCartPopup: true,
                            },
                            meta: {
                                id: 'marketfront-4195',
                            },
                            pageObjects: {
                                parent() {
                                    return this.createPageObject(TopOfferSnippetCompact);
                                },
                                cartButton() {
                                    return this.createPageObject(CartButton, {
                                        parent: TopOfferSnippetCompact.root,
                                    });
                                },
                                counterCartButton() {
                                    return this.createPageObject(CounterCartButton, {
                                        parent: TopOfferSnippetCompact.root,
                                    });
                                },
                                cartPopup() {
                                    return this.createPageObject(CartPopup);
                                },
                            },
                        }),
                        {
                            'Блок кешбэка.': mergeSuites(
                                {
                                    beforeEach() {
                                        this.setPageObjects({
                                            cashbackDealTerms() {
                                                return this.createPageObject(CashbackDealTerms, {
                                                    parent: `${TopOfferSnippetCompact.priceAndShop} ${OfferPrice.root}`,
                                                });
                                            },
                                            cashbackInfoTooltip() {
                                                // Выбираем тултип кешбэка более точечно, так как у нас на этой странице
                                                // есть точно такие же тултипы кешбэка (CashbackInfoTooltip) в попапе
                                                // и мы не хотим чтобы они пересекались.
                                                return this.createPageObject(HintTooltip, {
                                                    parent: OfferPrice.root,
                                                });
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
                                        id: 'marketfront-4092',
                                    },
                                    hooks: {
                                        async beforeEach() {
                                            await this.browser.setState('report', productWithTop6CashbackOffer.state);
                                            await this.browser.yaLogin(
                                                profiles['pan-topinambur'].login,
                                                profiles['pan-topinambur'].password
                                            );
                                            return this.browser.yaOpenPage('market:product', productWithTop6CashbackOffer.route);
                                        },
                                    },
                                    params: {
                                        cashbackAmount: CASHBACK_AMOUNT_TOP6,
                                        cashbackFormat: 'short',
                                        isTooltipOnHover: false,
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
                                            await this.browser.setState('report', productWithTop6ExtraCashbackOffer.state);
                                            await this.browser.yaLogin(
                                                profiles['pan-topinambur'].login,
                                                profiles['pan-topinambur'].password
                                            );
                                            return this.browser.yaOpenPage('market:product', productWithTop6ExtraCashbackOffer.route);
                                        },
                                    },
                                    params: {
                                        cashbackAmount: CASHBACK_AMOUNT_TOP6,
                                        cashbackFormat: 'short',
                                        isTooltipOnHover: false,
                                        isExtraCashback: true,
                                    },
                                })
                            ),
                        }
                    ),
                }),
                makeSuite('DSBS-оффер в топ-6', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', productWithTop6OfferDSBS.state);

                                return this.browser.yaOpenPage('market:product', productWithTop6OfferDSBS.route);
                            },
                        },
                        prepareSuite(ItemCounterCartButtonSuite, {
                            params: {
                                counterStep: 1,
                                offerId: productWithTop6OfferDSBS.dsbsOffer.wareId,
                                withCartPopup: true,
                            },
                            meta: {
                                id: 'marketfront-4353',
                            },
                            pageObjects: {
                                parent() {
                                    return this.createPageObject(TopOfferSnippetCompact);
                                },
                                cartButton() {
                                    return this.createPageObject(CartButton, {
                                        parent: TopOfferSnippetCompact.root,
                                    });
                                },
                                counterCartButton() {
                                    return this.createPageObject(CounterCartButton, {
                                        parent: TopOfferSnippetCompact.root,
                                    });
                                },
                                cartPopup() {
                                    return this.createPageObject(CartPopup);
                                },
                            },
                        }),
                        prepareSuite(DsbsCompactOfferSuite, {
                            params: {
                                shopName: productWithTop6OfferDSBS.dsbsOffer.shop.name,
                                gradesCount: '3K',
                                price: Number(productWithTop6OfferDSBS.dsbsOffer.prices.value),
                            },
                        })
                    ),
                })
            ),
        }),

        makeSuite('Регион в ДО.', {
            story: mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            defaultOfferTitle: () => this.createPageObject(DefaultOfferTitle),
                            region: () => this.createPageObject(Region),
                        });

                        return this.browser
                            .yaOpenPage('market:compare')
                            .then(() => this.region.openForm())
                            .then(() => this.browser.waitForVisible(Region.suggest))
                            .then(() => this.region.setNewRegion('Самара'))
                            .then(() => this.browser.waitForVisible(Region.selectFormList))
                            .then(() => this.region.getSuggestItemByIndex(1).click())
                            .then(() => this.region.applyNewRegionFromButton());
                    },
                }
            ),
        }),
        makeSuite('Форма обратной связи в ДО', {
            environment: 'kadavr',
            story: prepareSuite(PopupComplainSuite, {
                pageObjects: {
                    popup() {
                        return this.createPageObject(ComplainButton, {
                            parent: ProductDefaultOffer.root,
                        });
                    },
                    popupForm() {
                        return this.createPageObject(PopupComplainForm);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await Promise.all([
                            this.browser.setState('report', productWithExternalLinkInDO.state),
                            this.browser.setState('Forms.data.collections.forms', {
                                [shopFeedbackFormId]: createSurveyFormMock(shopFeedbackFormId),
                            }),
                        ]);

                        await this.browser.yaOpenPage('market:product', productWithExternalLinkInDO.route);
                    },
                },
            }),
        }),

        makeSuite('Форма обратной связи в ТОП-6', {
            environment: 'kadavr',
            story: prepareSuite(PopupComplainSuite, {
                pageObjects: {
                    popup() {
                        return this.createPageObject(ComplainButton, {
                            parent: `${MiniTopOffers.item}:nth-child(1)`,
                        });
                    },
                    popupForm() {
                        return this.createPageObject(PopupComplainForm);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await Promise.all([
                            this.browser.setState('report', productWithTop6Offer.state),
                            this.browser.setState('Forms.data.collections.forms', {
                                [shopFeedbackFormId]: createSurveyFormMock(shopFeedbackFormId),
                            }),
                        ]);

                        await this.browser.yaOpenPage('market:product', productWithTop6Offer.route);
                    },
                },
            }),
        }),

        makeSuite('Дефолтный оффер', {
            environment: 'kadavr',
            feature: 'Дефолтный оффер',
            story: mergeSuites(
                makeSuite('Дефолтный офффер, указанный в ссылке через параметр do-waremd5', {
                    environment: 'kadavr',
                    issue: 'MARKETFRONT-34303',
                    story: mergeSuites(
                        prepareSuite(ProductDefaultOfferValidOfferIdSuite, {
                            params: {
                                expectedOfferId: productWithWareMd5Offer.defaultOfferId,
                            },
                            hooks: {
                                async beforeEach() {
                                    await this.browser.setState('report', productWithWareMd5Offer.state);
                                    return this.browser.yaOpenPage('market:product', productWithWareMd5Offer.route);
                                },
                            },
                        })
                    ),
                }),
                prepareSuite(CashbackDealTermSuite, {
                    pageObjects: {
                        cashbackDealTerms() {
                            return this.createPageObject(CashbackDealTerms, {
                                parent: ProductDefaultOffer.root,
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
                    },
                    meta: {
                        id: 'marketfront-4178',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithCashbackInDO.state);
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password
                            );
                            return this.browser.yaOpenPage('market:product', productWithCashbackInDO.route);
                        },
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT_DO,
                        cashbackFormat: 'full',
                        isTooltipOnHover: true,
                        isExtraCashback: false,
                    },
                }),

                prepareSuite(CartUpsalePopupSuite),
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
                        expectedText: '1 уп = 1.248 м²',
                    },
                    meta: {
                        id: 'marketfront-5766',
                        issue: 'MARKETFRONT-79800',
                    },
                    hooks: {
                        async beforeEach() {
                            const offer = createOfferForProduct(
                                {
                                    ...oneOfferCpa.offerMock,
                                    promos: [spreadDiscountCountPromo],
                                    cpa: 'real',
                                    benefit: {
                                        type: 'recommended',
                                        description: 'Хорошая цена от надёжного магазина',
                                        isPrimary: true,
                                    },
                                    unitInfo: unitInfo,
                                },
                                oneOfferCpa.params.productId,
                                oneOfferCpa.offerId
                            );
                            const state = mergeState([oneOfferCpa.reportState, offer]);

                            await this.browser.setState('report', state);
                            await this.browser.setState(
                                `report.collections.offer.${oneOfferCpa.offerMock.wareId}.navnodes`,
                                [{
                                    ...oneOfferCpa.offerMock.navnodes[0],
                                    tags: ['unit_calc'],
                                }]
                            );
                            await this.browser.yaOpenPage(
                                'market:product',
                                oneOfferCpa.params
                            );
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

                            const offer = createOfferForProduct(
                                {
                                    ...oneOfferCpa.offerMock,
                                    promos: [spreadDiscountCountPromo],
                                    cpa: 'real',
                                    benefit: {
                                        type: 'recommended',
                                        description: 'Хорошая цена от надёжного магазина',
                                        isPrimary: true,
                                    },
                                    unitInfo: unitInfo,
                                    navnodes: [{
                                        ...oneOfferCpa.offerMock.navnodes[0],
                                        id: 55555,
                                        tags: ['unit_calc'],
                                    }],
                                },
                                oneOfferCpa.params.productId,
                                oneOfferCpa.offerId
                            );
                            const state = mergeState([oneOfferCpa.reportState, offer]);

                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:product', oneOfferCpa.params);

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

                prepareSuite(CashbackDealTermSuite, {
                    suiteName: 'Повышенный кешбэк.',
                    pageObjects: {
                        cashbackDealTerms() {
                            return this.createPageObject(CashbackDealTerms, {
                                parent: ProductDefaultOffer.root,
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
                    },
                    meta: {
                        id: 'marketfront-4499',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithExtraCashbackInDO.state);
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password
                            );
                            return this.browser.yaOpenPage('market:product', productWithExtraCashbackInDO.route);
                        },
                    },
                    params: {
                        cashbackAmount: CASHBACK_AMOUNT_DO,
                        cashbackFormat: 'full',
                        isTooltipOnHover: true,
                        isExtraCashback: true,
                    },
                }),

                prepareSuite(setBlockData, {
                    pageObjects: {
                        productSummary() {
                            return this.createPageObject(ProductSummary);
                        },
                        offerSet() {
                            return this.createPageObject(OfferSet, {
                                parent: this.productSummary,
                            });
                        },
                    },
                    meta: {
                        id: 'marketfront-4235',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('report', productWithBlueSetInDO.state);
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password
                            );
                            return this.browser.yaOpenPage('market:product', productWithBlueSetInDO.route);
                        },
                    },
                }),

                prepareSuite(GenericBundleTermSuite, {
                    params: {
                        withPromoBlock: true,
                    },
                    pageObjects: {
                        promoBadge() {
                            return this.createPageObject(PromoBadge, {
                                parent: ProductDefaultOffer.root,
                            });
                        },
                    },
                    meta: {
                        id: 'marketfront-4274',
                    },
                    hooks: {
                        async beforeEach() {
                            const {
                                stateWithProductOffers,
                                primary,
                            } = prepareKadavrReportStateWithDefaultState();
                            await this.browser.setState('report', stateWithProductOffers);
                            await this.browser.setState('Carter.items', []);
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password
                            );
                            return this.browser.yaOpenPage('market:product', {
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
                                parent: ProductDefaultOffer.root,
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
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password
                            );
                            return this.browser.yaOpenPage('market:product', {
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
                                productId,
                                slug,
                            } = prepareKadavrReportStateForDirectDiscount();

                            await this.browser.setState('report', state);
                            await this.browser.setState('Carter.items', []);
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password
                            );
                            return this.browser.yaOpenPage('market:product', {
                                productId,
                                slug,
                            });
                        },
                    },
                })
            ),
        }),

        makeSuite('Дефолтный оффер CPA', {
            environment: 'kadavr',
            meta: {
                issue: 'MARKETFRONT-13236',
            },
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('Carter.items', []);
                        await this.browser.setState('report', productWithCPADO.state);

                        return this.browser.yaOpenPage('market:product', productWithCPADO.route);
                    },
                },
                prepareSuite(CartButtonSuite),
                prepareSuite(CartButtonCounterSuite),
                prepareSuite(ItemCounterCartButtonSuite, {
                    params: {
                        counterStep: productWithCPADO.offerMock.bundleSettings.quantityLimit.step,
                        offerId: productWithCPADO.offerMock.wareId,
                        withCartPopup: true,
                    },
                    pageObjects: {
                        parent() {
                            return this.createPageObject(ProductDefaultOffer);
                        },
                        cartButton() {
                            return this.createPageObject(CartButton, {
                                parent: ProductDefaultOffer.root,
                            });
                        },
                        counterCartButton() {
                            return this.createPageObject(CounterCartButton, {
                                parent: ProductDefaultOffer.root,
                            });
                        },
                        cartPopup() {
                            return this.createPageObject(CartPopup);
                        },
                    },
                })
            ),
        }),

        makeSuite('Ссылка «N вопросов о товаре» для товара с вопросами', {
            environment: 'kadavr',
            feature: 'Структура страницы',
            story: prepareSuite(ProductTitleWithQuestionsSuite, {
                meta: {
                    id: 'marketfront-3704',
                    issue: 'MARKETVERSTKA-35888',
                },
                hooks: {
                    async beforeEach() {
                        const product = productWithColoredPicturesFixture.state;
                        const {route} = productWithColoredPicturesFixture;
                        const {productId, slug} = route;

                        const question = createQuestion({
                            product: {
                                entity: 'product',
                                id: productId,
                            },
                        });
                        const schema = {
                            modelQuestions: [question],
                        };
                        await this.browser.setState('report', product);
                        await this.browser.setState('schema', schema);
                        return this.browser.yaOpenPage('market:product', {
                            productId,
                            slug,
                        });
                    },
                },
                pageObjects: {
                    productTitle() {
                        return this.createPageObject(ProductTitle);
                    },
                },
                params: {
                    productId: productWithColoredPicturesFixture.route.productId,
                    slug: productWithColoredPicturesFixture.route.slug,
                },
            }),
        }),

        makeSuite('Ссылка «N вопросов о товаре» для товара без вопросов', {
            environment: 'kadavr',
            feature: 'Структура страницы',
            story: prepareSuite(ProductTitleWithoutQuestionsSuite, {
                meta: {
                    id: 'marketfront-3705',
                    issue: 'MARKETVERSTKA-35891',
                },
                hooks: {
                    async beforeEach() {
                        const product = productWithColoredPicturesFixture.state;
                        const {route} = productWithColoredPicturesFixture;
                        const {productId, slug} = route;

                        await this.browser.setState('report', product);
                        return this.browser.yaOpenPage('market:product', {
                            productId,
                            slug,
                        });
                    },
                },
                pageObjects: {
                    productTitle() {
                        return this.createPageObject(ProductTitle);
                    },
                },
            }),
        }),

        prepareSuite(HeadBannerProductAbsenceSuite, {
            meta: {
                id: 'marketfront-3383',
                issue: 'MARKETVERSTKA-33961',
            },
            params: {
                pageId: 'market:product',
            },
        }),

        makeSuite('Диалог подтверждения возраста. Adult контент.', {
            environment: 'kadavr',
            feature: 'Диалог подтверждения возраста',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            adultConfirmationPopup() {
                                return this.createPageObject(AdultConfirmationPopup);
                            },
                        });

                        const product = createProduct(phone, phone.id);
                        const state = mergeState([
                            product,
                            {
                                data: {
                                    search: {adult: true},
                                },
                            },
                        ]);
                        await this.browser.setState('report', state);

                        return this.browser.yaOpenPage('market:product', {
                            productId: phone.id,
                            slug: phone.slug,
                        });
                    },
                },
                prepareSuite(AdultWarningDefaultSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-869',
                    },
                }),
                prepareSuite(AdultWarningAcceptSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-873',
                    },
                }),
                prepareSuite(AdultWarningDeclineSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-877',
                    },
                })
            ),
        }),

        makeSuite('Виджет UGC Медиа галереи.', {
            environment: 'kadavr',
            story: mergeSuites(
                prepareSuite(UgcMediaGallerySuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', reviewWithCommentAndPhotos.reportProduct)
                                .setState('schema', reviewWithCommentAndPhotos.oneReviewSchemaWithCommentary);


                            const testUser = profiles['pan-topinambur'];
                            await this.browser.yaLogin(
                                testUser.login,
                                testUser.password
                            );

                            await this.browser.yaOpenPage('market:product', {
                                productId: reviewWithCommentAndPhotos.product.productId,
                                slug: reviewWithCommentAndPhotos.product.slug,
                            });

                            await this.browser.yaSlowlyScroll(UgcMediaGallery.root);
                        },
                    },
                    pageObjects: {
                        ugcMediaGallery() {
                            return this.createPageObject(UgcMediaGallery);
                        },
                    },
                    params: {
                        productId: reviewWithCommentAndPhotos.product.productId,
                        slug: reviewWithCommentAndPhotos.product.slug,
                    },
                }),
                prepareSuite(UgcMediaGalleryVideoSuite, {
                    hooks: {
                        async beforeEach() {
                            const testUser = profiles['pan-topinambur'];

                            const {videos, videoVotes} = createUserProductVideos({
                                userId: testUser.uid,
                                productId: reviewWithComment.product.productId,
                            });

                            await this.browser
                                .setState('report', reviewWithComment.reportProduct)
                                .setState('schema', {
                                    ...reviewWithComment.oneReviewSchemaWithCommentary,
                                    ugcvideo: videos,
                                });
                            await this.browser.setState('storage', {videoVotes});

                            await this.browser.yaLogin(
                                testUser.login,
                                testUser.password
                            );

                            await this.browser.yaOpenPage('market:product', {
                                productId: reviewWithComment.product.productId,
                                slug: reviewWithComment.product.slug,
                            });

                            await this.browser.yaSlowlyScroll(UgcMediaGallery.root);
                        },
                    },
                    pageObjects: {
                        ugcMediaGallery() {
                            return this.createPageObject(UgcMediaGallery);
                        },
                        ugcGalleryFloatingReview() {
                            return this.createPageObject(UgcGalleryFloatingReview);
                        },
                    },
                    params: {
                        productId: reviewWithComment.product.productId,
                        slug: reviewWithComment.product.slug,
                        videoId: DEFAULT_VIDEO_ID,
                    },
                })
            ),
        }),

        prepareSuite(TopOfferFull, {
            meta: {
                id: 'marketfront-4205',
                issue: 'MARKETFRONT-25074',
            },
            pageObjects: {
                topOffersList() {
                    return this.createPageObject(TopOffers);
                },
                topOfferActions() {
                    return this.createPageObject(TopOfferActions, {
                        parent: this.topOffersList,
                        root: `${TopOfferSnippet.root}:nth-child(1)`,
                    });
                },
                shopName() {
                    return this.createPageObject(ShopName, {
                        parent: this.topOffersList,
                        root: `${TopOfferSnippet.root}:nth-child(1)`,
                    });
                },
            },
            params: {
                url: productWithTop6Offer.encryptedUrl,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithTop6Offer.state);
                    return this.browser.yaOpenPage('market:product-reviews', productWithTop6Offer.route);
                },
            },
        }),

        prepareSuite(DeliveryBetterWithPlus, {
            params: {
                pageId: 'market:product',
            },
            pageObjects: {
                freeDeliveryWithPlusLink() {
                    return this.createPageObject(FreeDeliveryWithPlusLink);
                },
            },
        }),

        credit,
        bnplSuite,
        pickup,
        metrika,
        seo,
        /**
         * @expFlag dsk_km-do_trust-rev
         * @ticket MARKETFRONT-71593
         * @start
         */
        // починить после окончания редизайна сниппетов КМ, когда появятся новые логотипы, сейчас их нет.
        // shopLogoDefaultOffer,
        shopLogoTop6,
        paOnSaleSubscription,
        ageConfirmation,
        automticallyCalculatedDelivery,
        promo,
        whitePreorder,
        preorder,
        placementTypesDefaultOffer,
        recommendedOffers,
        secondaryExpressOffer,
        vendorLine,
        unitInfoDefaultOfferSuite,
        unitInfoTop6Suite,
        unitInfoCartPopup,
        prepareSuite(ProductDegradationSuite, {
            environment: 'testing',
            pageObjects: {
                productSummary() {
                    return this.createPageObject(ProductSummary);
                },
                productTitle() {
                    return this.createPageObject(ProductTitle);
                },
            },
            hooks: {
                async beforeEach() {
                    return this.browser.yaOpenPage('market:product', routes.product.withRating);
                },
            },
        }),
        virtualPack
    ),
});
