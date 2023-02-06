import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import CameraInputSuite from '@self/platform/spec/hermione/test-suites/blocks/CameraInput';
import BubbleCategoriesSuite from '@self/platform/spec/hermione/test-suites/blocks/bubbleCategories';
import RegionPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/RegionPopup';
import HeadBannerPresenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/presence';
import HeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/Header';
import CartEntryPointSuit from '@self/project/src/spec/hermione/test-suites/blocks/CartEntryPoint';
import HeaderMenuTriggerSuite from '@self/platform/spec/hermione/test-suites/blocks/Header/__menuTrigger';
import SideMenuSuite from '@self/platform/spec/hermione/test-suites/blocks/SideMenu';
import SearchFormSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchForm';
import FooterSuite from '@self/platform/spec/hermione/test-suites/blocks/Footer';
import RollSuite from '@self/platform/spec/hermione/test-suites/blocks/Roll';
import EComQuestionsSuite from '@self/root/src/spec/hermione/test-suites/blocks/ecomQuestions';
import ScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox';
import ScrollBoxSnippetTransitionSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/__snippet/transition';
import ScrollBoxSnippetLinksSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/__snippet/links';
import ReviewSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewSnippet';
import RollOfferComplaintSuite from '@self/platform/spec/hermione/test-suites/blocks/Roll/OfferComplaint';
import cashback from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/deals/cashback';
import genericBundle from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/deals/genericBundle';
import verifiedCancellationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifiedCancellation';
import cancellationRejectionPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/cancellationRejection';
import removedItemsVerificationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedItemsVerification';
import removedOrderItemsPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedOrderItems';
import deliveryReschedulePopupSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/deliveryReschedulePopup';
import verifyDeliveryRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryReschedule';
import verifyDeliveryLastMileRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryLastMileReschedule';
import LoyaltyNotificationTooltip from '@self/root/src/spec/hermione/test-suites/touch.blocks/loyaltyNotificationTooltip/index.js';
import {expressEntryointsSuite} from '@self/platform/spec/hermione/test-suites/blocks/expressEntrypoints';
import cashbackInfoPopupInHeader from '@self/root/src/spec/hermione/test-suites/blocks/cashback/infoPopupInHeader';
/**
 * @expFlag touch_smart-banner_10_21
 * @ticket MARKETFRONT-59009
 * next-line
 */
import smartBanner from '@self/root/src/spec/hermione/test-suites/touch.blocks/smartBanner';
import appPromoFooter from '@self/platform/spec/hermione/test-suites/blocks/AppPromoFooter';
import fullScreenPopup from '@self/root/src/spec/hermione/test-suites/touch.blocks/fullScreenPopup';

// page-objects
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.touch';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import HeadBanner from '@self/platform/spec/page-objects/HeadBanner';
import SearchForm from '@self/platform/spec/page-objects/widgets/SearchForm';
import EComQuestion from '@self/root/src/widgets/content/EComQuestions/components/Question/__pageObject';
import EComQuestionOptions from '@self/root/src/widgets/content/EComQuestions/components/Options/__pageObject';
import EComQuestionInputPopup from '@self/root/src/widgets/content/EComQuestions/components/InputPopup/__pageObject';
import BubbleCategories from '@self/platform/spec/page-objects/BubbleCategories';
import Footer from '@self/platform/spec/page-objects/Footer';
import Roll from '@self/platform/spec/page-objects/Roll';
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import ReviewSnippet from '@self/platform/spec/page-objects/ReviewSnippet';
import CameraInput from '@self/platform/spec/page-objects/components/CameraInput';

import indexPageMock from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/fixtures/index-page';
import {expressEntrypointConfigMock} from '@self/root/src/spec/hermione/kadavr-mock/tarantino/express_entrypoint_config';
import PopupSlider from '@self/root/src/components/PopupSlider/__pageObject';
import Header2AddressSuite from '@self/root/src/spec/hermione/test-suites/blocks/express/header2-address';

import metrika from './metrika';
import multiAuth from './multiAuth';
import monobrand from './spVendors/monobrand';
import ugcPoll from './ugc-poll';


// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Главная страница.', {
    environment: 'testing',
    story: mergeSuites(
        prepareSuite(LoyaltyNotificationTooltip),
        prepareSuite(CameraInputSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('touch:index')
                        .yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                cameraInput() {
                    return this.createPageObject(CameraInput);
                },
            },
        }),

        prepareSuite(BubbleCategoriesSuite, {
            hooks: {
                beforeEach() {
                    const queryParams = routes.index;
                    return this.browser
                        .yaOpenPage('touch:index', queryParams)
                        .yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                bubbleCategories() {
                    return this.createPageObject(BubbleCategories);
                },
            },
        }),

        makeSuite('Главная страница.', {
            environment: 'testing',
            story: mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            touchRegionPopup: () => this.createPageObject(RegionPopup),
                        });
                        return this.browser.yaOpenPage('touch:index')
                            .yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },

                prepareSuite(RegionPopupSuite),

                prepareSuite(HeadBannerPresenceSuite, {
                    meta: {
                        id: 'm-touch-1934',
                        issue: 'MOBMARKET-7716',
                    },
                    pageObjects: {
                        headBanner() {
                            return this.createPageObject(HeadBanner);
                        },
                    },
                }),

                prepareSuite(HeaderSuite, {
                    pageObjects: {
                        header() {
                            return this.createPageObject(Header);
                        },
                    },
                }),

                prepareSuite(CartEntryPointSuit, {
                    meta: {
                        id: 'm-touch-3349',
                        issue: 'MARKETFRONT-13235',
                    },
                    pageObjects: {
                        header() {
                            return this.createPageObject(Header);
                        },
                        cartEntryPoint() {
                            return this.createPageObject(CartEntryPoint, {
                                parent: this.header,
                            });
                        },
                    },
                }),

                prepareSuite(HeaderMenuTriggerSuite, {
                    pageObjects: {
                        header() {
                            return this.createPageObject(Header);
                        },
                    },
                }),

                prepareSuite(SideMenuSuite, {
                    pageObjects: {
                        sideMenu() {
                            return this.createPageObject(SideMenu);
                        },
                    },
                }),

                prepareSuite(SearchFormSuite, {
                    pageObjects: {
                        search() {
                            return this.createPageObject(SearchForm);
                        },
                    },
                    params: {
                        routeParams: routes.catalog.tile,
                    },
                }),

                prepareSuite(FooterSuite, {
                    pageObjects: {
                        footer() {
                            return this.createPageObject(Footer);
                        },
                    },
                }),

                prepareSuite(RollSuite, {
                    meta: {
                        id: 'm-touch-2359',
                        issue: 'MOBMARKET-5456',
                    },
                    pageObjects: {
                        roll() {
                            return this.createPageObject(Roll);
                        },
                    },
                }),

                prepareSuite(RollOfferComplaintSuite, {
                    meta: {
                        issue: 'MOBMARKET-9871',
                        id: 'm-touch-3265',
                    },
                }),

                makeSuite('Блок "Вы смотрели похожее".', {
                    story: mergeSuites(
                        {
                            beforeEach() {
                                return this.browser
                                    .yaProfile('dzot61', 'touch:index');
                            },
                            afterEach() {
                                return this.browser.yaLogout();
                            },
                        },
                        prepareSuite(ScrollBoxSuite, {
                            pageObjects: {
                                ScrollBox() {
                                    return this.createPageObject(
                                        ScrollBox,
                                        {
                                            parent: '[data-zone-data*="Вы смотрели похожее"]',
                                        }
                                    );
                                },
                            },
                            meta: {
                                id: 'm-touch-1934',
                                issue: 'MOBMARKET-7716',
                            },
                        }),
                        prepareSuite(ScrollBoxSnippetTransitionSuite, {
                            pageObjects: {
                                ScrollBox() {
                                    return this.createPageObject(
                                        ScrollBox,
                                        {
                                            parent: '[data-zone-data*="Вы смотрели похожее"]',
                                        }
                                    );
                                },
                            },
                            meta: {
                                id: 'm-touch-1935',
                                issue: 'MOBMARKET-7717',
                            },
                            params: {
                                pathname: '\\/product--[\\w-]+\\/[0-9]+',
                            },
                        })
                    ),
                }),

                makeSuite('Блок "Приглядитесь к этим предложениям".', {
                    story: mergeSuites(
                        prepareSuite(ScrollBoxSuite, {
                            pageObjects: {
                                ScrollBox() {
                                    return this.createPageObject(
                                        ScrollBox,
                                        {
                                            parent: '[data-zone-data*="AttractiveModels"]',
                                        }
                                    );
                                },
                            },
                            meta: {
                                id: 'm-touch-1934',
                                issue: 'MOBMARKET-7716',
                            },
                            hooks: {
                                beforeEach() {
                                    return this.browser
                                        .yaOpenPage('touch:index');
                                },
                            },
                        }),
                        prepareSuite(ScrollBoxSnippetLinksSuite, {
                            pageObjects: {
                                ScrollBox() {
                                    return this.createPageObject(
                                        ScrollBox,
                                        {
                                            parent: '[data-zone-data*="AttractiveModels"]',
                                        }
                                    );
                                },
                            },
                            meta: {
                                id: 'm-touch-2335',
                                issue: 'MOBMARKET-9439',
                            },
                            hooks: {
                                beforeEach() {
                                    return this.browser
                                        .yaOpenPage('touch:index');
                                },
                            },
                            params: {
                                pathname: '\\/product--[\\w-]+\\/[0-9]+',
                            },
                        })
                    )}),

                makeSuite('Отзывы на лучшие товары.', {
                    environment: 'testing',
                    story: prepareSuite(ReviewSnippetSuite, {
                        hooks: {
                            beforeEach() {
                                return this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                            },
                        },
                        pageObjects: {
                            reviewSnippet() {
                                return this.createPageObject(ReviewSnippet);
                            },
                        },
                    }),
                }),
                prepareSuite(Header2AddressSuite)
            ),
        }),

        prepareSuite(deliveryReschedulePopupSuite, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
        }),

        prepareSuite(EComQuestionsSuite, {
            params: {
                isAuthWithPlugin: true,
            },
            pageObjects: {
                widget() {
                    return this.createPageObject(EComQuestion);
                },
                options() {
                    return this.createPageObject(EComQuestionOptions, {
                        parent: this.widget,
                    });
                },
                inputPopup() {
                    return this.createPageObject(EComQuestionInputPopup);
                },
            },
        }),

        prepareSuite(verifyDeliveryRescheduleSuite, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
        }),

        prepareSuite(verifyDeliveryLastMileRescheduleSuite, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
        }),

        prepareSuite(verifiedCancellationPopup, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
        }),

        prepareSuite(cancellationRejectionPopup, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
        }),

        prepareSuite(removedItemsVerificationPopup, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [indexPageMock]
                    );
                },
            },
        }),

        prepareSuite(removedOrderItemsPopup, {
            params: {
                pageId: PAGE_IDS_COMMON.INDEX,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [indexPageMock]
                    );
                },
            },
        }),

        prepareSuite(cashbackInfoPopupInHeader, {
            pageObjects: {
                header2() {
                    return this.createPageObject(Header);
                },
                popupModal() {
                    return this.createPageObject(PopupSlider);
                },
            },
        }),

        metrika,
        multiAuth,
        cashback,
        monobrand,
        genericBundle,
        ugcPoll,

        prepareSuite(expressEntryointsSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [indexPageMock, expressEntrypointConfigMock]
                    );

                    await this.browser.yaOpenPage('market:index', routes.region.baseLocation);
                },
            },
        }),

        /**
         * @expFlag touch_smart-banner_10_21
         * @ticket MARKETFRONT-59009
         * @ifSuccess Раскомментить тест сьют
         *
         * next-line
         */
        smartBanner,
        appPromoFooter,
        fullScreenPopup

        /**
         * TODO: расскоментировать при выкатке эксперимента
         * https://st.yandex-team.ru/MOBMARKET-8891.
         */
        /* makeSuite('Главная страница с кадавром', {
            story: mergeSuites(
                prepareSuite(import SubscriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/Subscription';, {
                    pageObjects: {
                        subscription() {
                            return this.createPageObject('Subscription');
                        },
                    },
                })
            ),
        }) */
    ),
});
