import _ from 'lodash';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {
    createProduct,
    createOfferForProduct,
    createFilter,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createQuestion} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createOffer} from '@self/platform/spec/hermione/helpers/shopRating';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {createSurveyFormMock} from '@self/project/src/spec/hermione/helpers/metakadavr';
import {shopFeedbackFormId} from '@self/platform/spec/hermione/configs/forms';
// suites
import FilterPriceSuite from '@self/platform/spec/hermione/test-suites/blocks/FilterPrice';
import {OldFilterCounter as FilterCounterSuite} from '@self/platform/spec/hermione/test-suites/blocks/FilterCounter';
import ProductOffersFiltersAsideBooleanSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersFiltersAside/boolean';
// import OfferDetailsButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/n-i-offer-details-button';
import PopupComplainSuite from '@self/platform/spec/hermione/test-suites/blocks/popup-complain';
import ProductSubscriptionPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup';
import ProductSubscriptionPopupPriceDropOpenerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup/priceDropOpener';
import ProductSubscriptionPopupConfirmedFormSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup/confirmedForm';
import ProductSubscriptionPopupEmailConfirmationFormSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductSubscriptionPopup/emailConfirmationForm';
import ProductOffersRegionHeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-offers-region-header';
import ProductOffersListItemSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-offers-list-item';
import HeadBannerProductAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/productAbsence';
import ProductTitleWithQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-title/withQuestions';
import ProductTitleWithoutQuestionsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-title/withoutQuestions';
import ShopInfoDrugsDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/drugsDisclaimer';
import AdultWarningDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/default';
import AdultWarningAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/accept';
import AdultWarningDeclineSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/decline';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import CashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/Cashback';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
// убрали бейдж подарка в рамках эксп expt_promo_top6
// import GenericBundleTermSuite from '@self/platform/spec/hermione/test-suites/blocks/GenericBundle';
import DSBSOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-offers-list-item/dsbs';

// page-objects
import ProductOffersFiltersAside from '@self/platform/spec/page-objects/ProductOffersFiltersAside';
import SnippetList from '@self/platform/widgets/content/productOffers/Results/__pageObject';
import FilterList from '@self/platform/spec/page-objects/FilterList';
import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';
import ComplainButton from '@self/platform/spec/page-objects/components/ComplainButton';
import ComplainPopup from '@self/platform/spec/page-objects/components/ComplainPopup';
import Region from '@self/platform/spec/page-objects/region';
import Header2Nav from '@self/platform/spec/page-objects/header2-nav';
import Header2ProfileMenu from '@self/platform/spec/page-objects/header2-profile-menu';
import ClickoutButton from '@self/platform/spec/page-objects/components/ClickoutButton';
import MiniCard from '@self/platform/components/PageCardTitle/MiniCard/__pageObject';
import MorePricesLink from '@self/platform/widgets/content/MorePricesLink/__pageObject';
import ProductSubscriptionPopup from '@self/platform/spec/page-objects/ProductSubscriptionPopup';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

import ReactPriceDropOpener from '@self/platform/widgets/content/PriceDropSubscription/__pageObject';
import ProductOffersRegionHeader from '@self/platform/widgets/pages/ProductOffersPage/__pageObject/regionTitle';

import DefaultOfferMini from '@self/platform/components/DefaultOfferMini/__pageObject';
import OfferPaymentTypes from '@self/platform/spec/page-objects/components/OfferPaymentTypes';
import ProductTitle from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';
import CashbackDealTerms from '@self/platform/spec/page-objects/components/CashbackDealTerms';
import CashbackInfoTooltip from '@self/platform/spec/page-objects/components/CashbackInfoTooltip';
import ProductOffer from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
import ShopName from '@self/project/src/components/ShopName/__pageObject';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
// убрали бейдж подарка в рамках эксп expt_promo_top6
// import DealsSticker from '@self/platform/spec/page-objects/DealsSticker';

// mocks
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import {guruMock, bookMock, groupMock, clusterMock} from '@self/platform/spec/hermione/fixtures/priceFilter/product';
import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';
import {filterPriceMock} from '@self/platform/spec/hermione/fixtures/priceFilter/filterPrice';
import offerFixture from './fixtures/offer';
import productOptionsFixture from './fixtures/productOptions';
import dataFixture from './fixtures/data';
import oneOffer from './fixtures/oneOffer';
import oneOfferCPA from './fixtures/oneOfferCPA';
import oneOfferDSBS from './fixtures/oneOfferDSBS';
import oneOfferWithCashback from './fixtures/oneOfferWithCashback';
import oneOfferWithExtraCashback from './fixtures/oneOfferWithCashback';
// убрали бейдж подарка в рамках эксп expt_promo_top6
// import oneOfferWithGenericBundle from './fixtures/oneOfferWithGenericBundle';
// imports
import FilterColor from './filters/filterColor';
import FilterPayments from './filters/filterPayments';
import FilterCredit from './filters/filterCredit';
import ShopInfo from './shop-info';
import ShopLogo from './shopLogo';
import ageConfirmation from './ageConfirmation';
import credit from './credit';
import Promo from './promo';
import pickup from './pickup';
import seo from './seo';
import miniCard from './miniCard';
import defaultOffer from './defaultOffer';
import sku from './sku';
import cashbackOnMiniCard from './cashbackOnMiniCard';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница карточки модели, цены.', {
    environment: 'testing',
    story: mergeSuites(
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

                        const productId = 12345;
                        const state = mergeState([
                            stateProductWithDO(productId, {
                                type: 'model',
                                titles: {
                                    raw: 'Смартфон Apple iPhone 7 256GB',
                                },
                            }),
                            {
                                data: {
                                    search: {adult: true},
                                },
                            },
                        ]);

                        await this.browser.setState('report', state);

                        return this.browser.yaOpenPage('market:product-offers', {
                            slug: productOptionsFixture.slug,
                            productId,
                        });
                    },
                },
                prepareSuite(AdultWarningDefaultSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4032',
                    },
                }),
                prepareSuite(AdultWarningAcceptSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4037',
                    },
                }),
                prepareSuite(AdultWarningDeclineSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4042',
                    },
                })
            ),
        }),
        makeSuite('Фильтрация.', {
            environment: 'kadavr',
            story: _.merge(
                createStories(
                    [
                        guruMock,
                        bookMock,
                        groupMock,
                        clusterMock,
                    ],
                    ({mock}) => {
                        const suiteParams = {
                            pageObjects: {
                                filtersAside() {
                                    return this.createPageObject(ProductOffersFiltersAside);
                                },
                                snippetList() {
                                    return this.createPageObject(SnippetList);
                                },
                            },
                            params: {
                                isTestOfOldPriceFilter: true,
                            },
                        };

                        return mergeSuites(
                            {
                                async beforeEach() {
                                    const product = createProduct(_.assign({}, productOptionsFixture, mock), mock.id);
                                    const offer = createOfferForProduct(offerFixture, mock.id, '3');

                                    const state = mergeState([
                                        offer,
                                        product,
                                        createFilter(filterPriceMock, 'glprice'),
                                        dataFixture,
                                    ]);

                                    await this.browser.setState('report', state);

                                    return this.browser.yaOpenPage('market:product-offers', {
                                        productId: mock.id,
                                        slug: mock.slug,
                                    });
                                },
                                after() {
                                    return this.browser.deleteCookie('viewtype');
                                },
                            },
                            prepareSuite(FilterPriceSuite, suiteParams),
                            prepareSuite(FilterCounterSuite, suiteParams)
                        );
                    }
                ),

                prepareSuite(ProductOffersFiltersAsideBooleanSuite, {
                    pageObjects: {
                        filterList() {
                            return this.createPageObject(FilterList, {
                                root: `${FilterList.root}[data-autotest-id="promo-type"]`,
                            });
                        },
                        productTabs() {
                            return this.createPageObject(ProductTabs);
                        },
                        miniCard() {
                            return this.createPageObject(MiniCard);
                        },
                        morePricesLink() {
                            return this.createPageObject(MorePricesLink);
                        },
                    },
                }),

                FilterColor,
                FilterCredit,
                FilterPayments
            ),
        }),

        seo,
        prepareSuite(PopupComplainSuite, {
            meta: {
                environment: 'kadavr',
            },
            pageObjects: {
                popup() {
                    return this.createPageObject(ComplainButton, {parent: SnippetList.root});
                },
                popupForm() {
                    return this.createPageObject(ComplainPopup);
                },
            },
            hooks: {
                async beforeEach() {
                    await Promise.all([
                        this.browser.setState('report', oneOffer.reportState),
                        this.browser.setState('Forms.data.collections.forms', {
                            [shopFeedbackFormId]: createSurveyFormMock(shopFeedbackFormId),
                        }),
                    ]);

                    await this.browser.yaOpenPage('market:product-offers', oneOffer.params);
                },
            },
        }),

        makeSuite('Подписка на снижение цены.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        const productId = 12345;
                        const state = stateProductWithDO(productId, {
                            type: 'model',
                            titles: {
                                raw: 'Смартфон Apple iPhone 7 256GB',
                            },
                        });

                        await this.browser.setState('report', state);
                        await this.browser.setState('marketUtils.data.subscriptions', []);

                        await this.setPageObjects({
                            openerButton: () => this.createPageObject(MiniCard),
                            productSubscriptionPopup: () => this.createPageObject(ProductSubscriptionPopup),
                            priceDropOpener: () => this.createPageObject(ReactPriceDropOpener),
                        });

                        await this.browser.yaOpenPage('market:product-offers', {productId, slug: 'some-slug'});

                        await this.browser.allure.runStep(
                            'Чистим куку показа попапа Подписки на снижение цены',
                            () => this.browser.deleteCookie('priceDropSubscriptionShown'));

                        return this.browser.yaPageReload(10000, ['state']);
                    },

                    afterEach() {
                        return this.browser.deleteCookie('priceDropSubscriptionShown');
                    },
                },
                prepareSuite(ProductSubscriptionPopupSuite),
                {
                    'Для авторизованного пользователя.': mergeSuites(
                        {
                            async beforeEach() {
                                const productId = 12345;

                                const retPath = await this.browser.yaBuildURL('market:product-offers', {
                                    productId, slug: 'some-slug',
                                });

                                return this.browser.allure.runStep('Логинимся',
                                    () => this.browser.yaTestLogin(retPath));
                            },

                            afterEach() {
                                return this.browser.yaLogout();
                            },
                        },
                        prepareSuite(ProductSubscriptionPopupPriceDropOpenerSuite, {
                            params: oneOffer.params,
                        }),
                        prepareSuite(ProductSubscriptionPopupConfirmedFormSuite, {
                            meta: {
                                id: 'marketfront-2653',
                                issue: 'MARKETVERSTKA-33967',
                            },
                        }),
                        prepareSuite(ProductSubscriptionPopupEmailConfirmationFormSuite, {
                            meta: {
                                id: 'marketfront-3391',
                                issue: 'MARKETVERSTKA-33967',
                            },
                        })
                    ),

                    'Для неавторизованного пользователя.': prepareSuite(
                        ProductSubscriptionPopupEmailConfirmationFormSuite,
                        {
                            meta: {
                                id: 'marketfront-2654',
                                issue: 'MARKETVERSTKA-33967',
                            },
                        }
                    ),
                }
            ),
        }),

        makeSuite('Блок с оферами.', {
            story: mergeSuites(
                makeSuite('Регион', {
                    environment: 'testing',
                    story: prepareSuite(ProductOffersRegionHeaderSuite, {
                        hooks: {
                            async beforeEach() {
                                const params = {productId: 14206636, slug: 'smartfon-apple-iphone-7-32gb'};
                                return this.browser.yaOpenPage('market:product-offers', params);
                            },
                        },
                        pageObjects: {
                            headerNav() {
                                return this.createPageObject(Header2Nav);
                            },
                            profileMenu() {
                                return this.createPageObject(Header2ProfileMenu);
                            },
                            region() {
                                return this.createPageObject(Region);
                            },
                            productOffersRegionHeader() {
                                return this.createPageObject(ProductOffersRegionHeader);
                            },
                        },
                        params: {
                            city: 'Самара',
                            title: 'Предложения магазинов в Самаре',
                            isAuthWithPlugin: true,
                        },
                    }),
                }),
                makeSuite('Список офферов', {
                    environment: 'kadavr',
                    story: prepareSuite(ProductOffersListItemSuite, {
                        hooks: {
                            beforeEach() {
                                return this.browser.setState('report', oneOffer.reportState)
                                    .then(() => this.browser.yaOpenPage('market:product-offers', oneOffer.params));
                            },
                        },
                        pageObjects: {
                            clickoutButton() {
                                return this.createPageObject(ClickoutButton);
                            },
                            paymentType() {
                                return this.createPageObject(OfferPaymentTypes);
                            },
                            productOffer() {
                                return this.createPageObject(ProductOffer);
                            },
                            shopName() {
                                return this.createPageObject(ShopName, {
                                    parent: this.productOffer,
                                });
                            },
                        },
                        params: {
                            urlDomain: 'market-click2-testing.yandex.ru',
                            url: oneOffer.encryptedUrl,
                            paymentTypeText: 'Оплата наличными курьеру',
                        },
                    }),
                }),
                makeSuite('Оффер CPA в шапке', {
                    environment: 'kadavr',
                    meta: {
                        issue: 'MARKETFRONT-13236',
                    },
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', productWithCPADO.state);

                                return this.browser.yaOpenPage('market:product-offers', productWithCPADO.route);
                            },
                        },
                        prepareSuite(CartButtonSuite),
                        prepareSuite(CartButtonCounterSuite),
                        prepareSuite(ItemCounterCartButtonSuite, {
                            params: {
                                counterStep: productWithCPADO.offerMock.bundleSettings.quantityLimit.step,
                                offerId: productWithCPADO.offerMock.wareId,
                            },
                            meta: {
                                id: 'marketfront-4195',
                            },
                            pageObjects: {
                                parent() {
                                    return this.createPageObject(DefaultOfferMini);
                                },
                                cartButton() {
                                    return this.createPageObject(CartButton, {
                                        parent: DefaultOfferMini.root,
                                    });
                                },
                                counterCartButton() {
                                    return this.createPageObject(CounterCartButton, {
                                        parent: DefaultOfferMini.root,
                                    });
                                },
                                cartPopup() {
                                    return this.createPageObject(CartPopup);
                                },
                            },
                        })
                    ),
                }),

                makeSuite('Оффер CPA в ценах', {
                    environment: 'kadavr',
                    meta: {
                        issue: 'MARKETFRONT-13236',
                    },
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', oneOfferCPA.reportState);

                                return this.browser.yaOpenPage('market:product-offers', oneOfferCPA.params);
                            },
                            after() {
                                return this.browser.deleteCookie('viewtype');
                            },
                        },
                        prepareSuite(CartButtonSuite),
                        prepareSuite(CartButtonCounterSuite),
                        prepareSuite(ItemCounterCartButtonSuite, {
                            params: {
                                counterStep: oneOfferCPA.offerMock.bundleSettings.quantityLimit.step,
                                offerId: oneOfferCPA.offerMock.wareId,
                            },
                            meta: {
                                id: 'marketfront-4195',
                            },
                            pageObjects: {
                                parent() {
                                    return this.createPageObject(ProductOffer);
                                },
                                cartButton() {
                                    return this.createPageObject(CartButton, {
                                        parent: ProductOffer.root,
                                    });
                                },
                                counterCartButton() {
                                    return this.createPageObject(CounterCartButton, {
                                        parent: ProductOffer.root,
                                    });
                                },
                                cartPopup() {
                                    return this.createPageObject(CartPopup);
                                },
                            },
                        })
                    ),
                }),


                makeSuite('Оффер DSBS в ценах', {
                    environment: 'kadavr',
                    meta: {
                        issue: 'MARKETFRONT-13236',
                    },
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', oneOfferDSBS.reportState);

                                return this.browser.yaOpenPage('market:product-offers', oneOfferDSBS.params);
                            },
                            after() {
                                return this.browser.deleteCookie('viewtype');
                            },
                        },
                        prepareSuite(CartButtonSuite),
                        prepareSuite(CartButtonCounterSuite),
                        prepareSuite(ItemCounterCartButtonSuite, {
                            params: {
                                counterStep: oneOfferDSBS.offerMock.bundleSettings.quantityLimit.step,
                                offerId: oneOfferDSBS.offerMock.wareId,
                            },
                            meta: {
                                id: 'marketfront-4195',
                            },
                            pageObjects: {
                                parent() {
                                    return this.createPageObject(ProductOffer);
                                },
                                cartButton() {
                                    return this.createPageObject(CartButton, {
                                        parent: ProductOffer.root,
                                    });
                                },
                                counterCartButton() {
                                    return this.createPageObject(CounterCartButton, {
                                        parent: ProductOffer.root,
                                    });
                                },
                                cartPopup() {
                                    return this.createPageObject(CartPopup);
                                },
                            },
                        }),
                        prepareSuite(DSBSOfferSuite, {
                            params: {
                                urls: oneOfferDSBS.offerMock.urls,
                            },
                            hooks: {
                                beforeEach() {
                                    this.setPageObjects({
                                        snippet: () => this.createPageObject(
                                            ProductOffer,
                                            {
                                                root: `${ProductOffer.root}:nth-child(1)`,
                                            }
                                        ),
                                    });
                                },
                            },
                        })
                    ),
                })
            ),
        }),

        prepareSuite(HeadBannerProductAbsenceSuite, {
            meta: {
                id: 'marketfront-3385',
                issue: 'MARKETVERSTKA-33961',
            },
            params: {
                pageId: 'market:product-offers',
            },
        }),

        makeSuite('Ссылка «N вопросов о товаре» для товара с вопросами', {
            environment: 'kadavr',
            feature: 'Структура страницы',
            story: prepareSuite(ProductTitleWithQuestionsSuite, {
                meta: {
                    id: 'marketfront-3706',
                    issue: 'MARKETVERSTKA-35892',
                },
                hooks: {
                    async beforeEach() {
                        const {productId, slug} = oneOffer.params;

                        const question = createQuestion({
                            product: {
                                entity: 'product',
                                id: productId,
                                slug,
                            },
                        });
                        const schema = {
                            modelQuestions: [question],
                        };
                        await this.browser.setState('report', oneOffer.reportState);
                        await this.browser.setState('schema', schema);
                        return this.browser.yaOpenPage('market:product-offers', {
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
                    productId: oneOffer.params.productId,
                    slug: oneOffer.params.slug,
                },
            }),
        }),

        makeSuite('Ссылка «N вопросов о товаре» для товара без вопросов', {
            environment: 'kadavr',
            feature: 'Структура страницы',
            story: prepareSuite(ProductTitleWithoutQuestionsSuite, {
                meta: {
                    id: 'marketfront-3707',
                    issue: 'MARKETVERSTKA-35893',
                },
                hooks: {
                    async beforeEach() {
                        const {params} = oneOffer;
                        const {productId, slug} = params;

                        await this.browser.setState('report', oneOffer.reportState);
                        return this.browser.yaOpenPage('market:product-offers', {
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
                    const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                    const offer = createOffer({
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
                    }, offerId);
                    await this.browser.setState('report', offer);
                    return this.browser.yaOpenPage('market:offer', {offerId});
                },
            },
        }),

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
                id: 'marketfront-4180',
            },
            hooks: {
                async beforeEach() {
                    const {params} = oneOfferWithCashback;
                    const {productId, slug} = params;

                    await this.browser.setState('report', oneOfferWithCashback.reportState);
                    return this.browser.yaOpenPage('market:product-offers', {
                        productId,
                        slug,
                    });
                },
            },
            params: {
                cashbackAmount: oneOfferWithCashback.cashbackAmount,
                cashbackFormat: 'long',
                isTooltipOnHover: true,
                isExtraCashback: false,
            },
        }),
        prepareSuite(CashbackDealTermSuite, {
            suiteName: 'Повышенный кешбэк.',
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
                id: 'marketfront-4499',
            },
            hooks: {
                async beforeEach() {
                    const {params} = oneOfferWithExtraCashback;
                    const {productId, slug} = params;

                    await this.browser.setState('report', oneOfferWithExtraCashback.reportState);
                    return this.browser.yaOpenPage('market:product-offers', {
                        productId,
                        slug,
                    });
                },
            },
            params: {
                cashbackAmount: oneOfferWithCashback.cashbackAmount,
                cashbackFormat: 'long',
                isTooltipOnHover: true,
                isExtraCashback: true,
            },
        }),

        // prepareSuite(GenericBundleTermSuite, {
        //     pageObjects: {
        //         dealsSticker() {
        //             return this.createPageObject(DealsSticker);
        //         },
        //     },
        //     meta: {
        //         id: 'marketfront-4260',
        //     },
        //     hooks: {
        //         async beforeEach() {
        //             const {params, reportState} = oneOfferWithGenericBundle;
        //             const {productId, slug} = params;
        //
        //             await this.browser.setState('report', reportState);
        //             await this.browser.setState('Carter.items', []);
        //
        //             return this.browser.yaOpenPage('market:product-offers', {
        //                 productId,
        //                 slug,
        //             });
        //         },
        //     },
        // }),


        miniCard,
        cashbackOnMiniCard,
        ShopLogo,
        ShopInfo,
        ageConfirmation,
        credit,
        pickup,
        Promo,
        defaultOffer,
        sku
    ),
});
