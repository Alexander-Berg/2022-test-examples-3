import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import RegionSuite from '@self/platform/spec/hermione/test-suites/blocks/region';
import YandexSuggestSuite from '@self/platform/spec/hermione/test-suites/blocks/yandexSuggest';
import VertProductSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/VertProductSnippet';
import ScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox';
import Header2Suite from '@self/platform/spec/hermione/test-suites/blocks/header2';
import NavigationMenuTabSuite from '@self/platform/spec/hermione/test-suites/blocks/navigation-menu/tab';
import NavigationMenuTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/navigation-menu/title';
import NavigationMenuNodeLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/navigation-menu/nodeLink';
import NavigationMenuVerticalTabSuite from '@self/platform/spec/hermione/test-suites/blocks/navigation-menu/verticalTab';
import NavigationMenuGroupingTabSuite from '@self/platform/spec/hermione/test-suites/blocks/navigation-menu/groupingTab';
import CanonicalUrlSuite from '@self/platform/spec/hermione/test-suites/blocks/canonical-url';
import PageDescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/page-description';
import Header2NavSuite from '@self/platform/spec/hermione/test-suites/blocks/header2-nav';
import Header2ProfileMenuSuite from '@self/platform/spec/hermione/test-suites/blocks/header2-profile-menu';
import Header2AddressSuite from '@self/root/src/spec/hermione/test-suites/blocks/express/header2-address';
import CartEntryPointSuite from '@self/project/src/spec/hermione/test-suites/blocks/CartEntryPoint';
import CookiesLandingRegionSuite from '@self/platform/spec/hermione/test-suites/blocks/cookies/landingRegion';
import Search2LandingRegionCookieSuite from '@self/platform/spec/hermione/test-suites/blocks/search2/landingRegionCookie';
import cashbackDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/deal/cashback';
import cheapestAsGiftDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/deal/cheapestAsGift';
import genericBundleDealTermSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/deal/genericBundle';
import verifiedCancellationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifiedCancellation';
import removedItemsVerificationPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedItemsVerification';
import removedOrderItemsPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/removedOrderItems';
import cancellationRejectionPopup from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/cancellationRejection';
import deliveryReschedulePopupSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/deliveryReschedulePopup';
import verifyDeliveryRescheduleSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderIssues/verifyDeliveryReschedule';
import LoyaltyNotificationTooltip from '@self/root/src/spec/hermione/test-suites/desktop.blocks/loyaltyNotificationTooltip/index.js';
import yandexHelpTooltip from '@self/root/src/spec/hermione/test-suites/blocks/yandexHelp/yandexHelpTooltip';
import {expressEntryointsSuite} from '@self/platform/spec/hermione/test-suites/blocks/expressEntrypoints';
import cashbackInfoPopupInHeader from '@self/root/src/spec/hermione/test-suites/blocks/cashback/infoPopupInHeader';
import distributionFooterDesktop from '@self/root/src/spec/hermione/test-suites/desktop.blocks/distributionFooterDesktop';
import a11ySnippet from '@self/root/src/spec/hermione/test-suites/blocks/snippet/a11y';
// page-objects
import Region from '@self/platform/spec/page-objects/region';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import VertProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import Header2 from '@self/platform/spec/page-objects/header2';
import Header2Nav from '@self/platform/spec/page-objects/header2-nav';
import Header2Menu from '@self/platform/spec/page-objects/header2-menu';
import Header2ProfileMenu from '@self/platform/spec/page-objects/header2-profile-menu';
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.desktop';
import CanonicalUrl from '@self/platform/spec/page-objects/canonical-url';
import PageMeta from '@self/platform/spec/page-objects/pageMeta';
import MenuTab from '@self/platform/widgets/content/HeaderTabs/MenuTab/__pageObject';
import HeaderCatalog from '@self/platform/widgets/content/HeaderCatalog/__pageObject';
import CatalogTab from '@self/platform/widgets/content/HeaderCatalog/CatalogTab/__pageObject';
import MenuNavigation from '@self/platform/spec/page-objects/MenuNavigation';
import MenuNavigationDepartment from '@self/platform/spec/page-objects/MenuNavigationDepartment';
import HeaderRegionPopup from '@self/platform/widgets/content/HeaderRegionPopup/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';

import navigationRootMock from '@self/platform/spec/hermione/fixtures/navMenu/root.json';
import navigationCatalogMock from '@self/platform/spec/hermione/fixtures/navMenu/catalog.json';
import {expressEntrypointConfigMock} from '@self/root/src/spec/hermione/kadavr-mock/tarantino/express_entrypoint_config';

import COOKIE from '@self/root/src/constants/cookie';

import indexPageMock from './fixtures/index-page';
import banners from './banners';
import ugcPoll from './ugc-poll';
import monobrand from './spVendors/monobrand';
import productsWithCpaOffer from './fixtures/productsWithCpaOffer';
import indexPageWithAttractiveProductsMock from './fixtures/indexPageWithAttractiveProducts';


// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Главная страница.', {
    story: mergeSuites(
        prepareSuite(LoyaltyNotificationTooltip),
        prepareSuite(RegionSuite, {
            pageObjects: {
                region() {
                    return this.createPageObject(Region);
                },
            },
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('market:index', routes.region.ru);
                },

                /**
                 * INFO: При смене региона выставляется набор кук.
                 * Чтобы не удалять их точечно после проведения теста,
                 * просто открываем главную в зоне ru, переписывая все куки сразу.
                 */
                afterEach() {
                    return this.browser.yaOpenPage('market:index', routes.region.ru);
                },
            },
        }),
        makeSuite('Саджест.', {
            environment: 'testing',
            feature: 'Саджест',
            story: mergeSuites(
                {
                    async beforeEach() {
                        return this.browser.yaOpenPage('market:index');
                    },

                    async after() {
                        return this.browser.deleteCookie('exp_flags');
                    },
                },
                makeSuite('Исторические подсказки.', {
                    story: prepareSuite(YandexSuggestSuite),
                })
            ),

        }),

        makeSuite('Блок "приглядитесь к этим предложениям".', {
            feature: '"приглядитесь к этим предложениям" на морде',
            story: mergeSuites(
                makeSuite('Сниппеты.', {
                    environment: 'kadavr',
                    story: prepareSuite(VertProductSnippetSuite, {
                        pageObjects: {
                            scrollBox() {
                                return this.createPageObject(
                                    ScrollBox,
                                    {
                                        root: '[data-zone-data*="AttractiveModels"]',
                                    }
                                );
                            },
                            snippet() {
                                return this.createPageObject(VertProductSnippet, {
                                    parent: this.scrollBox.root,
                                });
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState(
                                    'Tarantino.data.result',
                                    [indexPageWithAttractiveProductsMock]
                                );

                                await this.browser.setState('report', productsWithCpaOffer);

                                return this.browser.yaOpenPage('market:index');
                            },
                        },
                    }),
                }),
                makeSuite('Скролл кнопками.', {
                    environment: 'kadavr',
                    story: prepareSuite(ScrollBoxSuite, {
                        meta: {
                            id: 'marketfront-2592',
                            issue: 'MARKETVERSTKA-29326',
                        },
                        pageObjects: {
                            scrollBox() {
                                return this.createPageObject(
                                    ScrollBox,
                                    {
                                        root: '[data-zone-data*="AttractiveModels"]',
                                    }
                                );
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState(
                                    'Tarantino.data.result',
                                    [indexPageWithAttractiveProductsMock]
                                );

                                await this.browser.setState('report', productsWithCpaOffer);

                                return this.browser.yaOpenPage('market:index');
                            },
                        },
                    }),
                })
            ),
        }),

        prepareSuite(expressEntryointsSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [indexPageMock, navigationRootMock, expressEntrypointConfigMock]
                    );


                    await this.browser.yaOpenPage('market:index', routes.region.ru);
                },
            },
        }),

        makeSuite('Хедер.', {
            environment: 'testing',
            story: mergeSuites(
                prepareSuite(Header2Suite, {
                    pageObjects: {
                        header2() {
                            return this.createPageObject(Header2);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:index');
                        },
                    },
                }),
                prepareSuite(CartEntryPointSuite, {
                    meta: {
                        id: 'marketfront-4073',
                        issue: 'MARKETFRONT-13256',
                    },
                    pageObjects: {
                        cartEntryPoint() {
                            return this.createPageObject(CartEntryPoint);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:index');
                        },
                    },
                })
            ),
        }),

        makeSuite('Навигационное меню.', {
            environment: 'kadavr',
            feature: 'Навигационное меню.',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState(
                            'Tarantino.data.result',
                            [indexPageMock, navigationRootMock, navigationCatalogMock]
                        );
                        await this.browser.yaOpenPage('market:index');
                        const headerCatalogEntrypoint =
                            this.createPageObject(Header2Menu, {root: Header2Menu.catalogEntrypoint});

                        await headerCatalogEntrypoint.clickCatalogAndWaitForVisible();
                    },
                },
                prepareSuite(NavigationMenuTabSuite, {
                    pageObjects: {
                        headerTab() {
                            return this.createPageObject(MenuTab);
                        },
                    },
                }),
                prepareSuite(NavigationMenuTitleSuite, {
                    pageObjects: {
                        navigationMenu() {
                            return this.createPageObject(MenuNavigation);
                        },
                    },
                }),
                prepareSuite(NavigationMenuNodeLinkSuite, {
                    pageObjects: {
                        nodeLinkGroup() {
                            return this.createPageObject(MenuNavigationDepartment);
                        },
                    },
                }),
                prepareSuite(NavigationMenuVerticalTabSuite, {
                    pageObjects: {
                        catalogTab() {
                            const headerCatalog = this.createPageObject(HeaderCatalog);

                            return this.createPageObject(CatalogTab, {
                                parent: headerCatalog,
                                root: `${CatalogTab.root}:nth-child(2)`,
                            });
                        },
                    },
                })
            ),
        }),

        makeSuite('Навигационное меню. Каталог.', {
            environment: 'kadavr',
            feature: 'Навигационное меню.',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState(
                            'Tarantino.data.result',
                            [indexPageMock, navigationRootMock, navigationCatalogMock]
                        );

                        return this.browser.yaOpenPage('market:index');
                    },
                },
                prepareSuite(NavigationMenuGroupingTabSuite, {
                    pageObjects: {
                        headerCatalogEntrypoint() {
                            return this.createPageObject(Header2Menu, {root: Header2Menu.catalogEntrypoint});
                        },
                        headerCatalog() {
                            return this.createPageObject(HeaderCatalog);
                        },
                        catalogTab() {
                            return this.createPageObject(CatalogTab);
                        },
                        firstNavigationMenu() {
                            return this.createPageObject(MenuNavigation);
                        },
                    },
                })
            ),
        }),

        makeSuite('SEO-разметка страницы.', {
            story: mergeSuites(
                prepareSuite(CanonicalUrlSuite, {
                    pageObjects: {
                        canonicalUrl() {
                            return this.createPageObject(CanonicalUrl);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            // INFO: страница прогресивная, поэтому используем дополнительно _mod=robot
                            return this.browser.yaOpenPage('market:index', {_mod: 'robot'});
                        },
                    },
                    params: {
                        expectedUrl: '/',
                    },
                }),
                prepareSuite(PageDescriptionSuite, {
                    hooks: {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:index');
                        },
                    },
                    pageObjects: {
                        pageMeta() {
                            return this.createPageObject(PageMeta);
                        },
                    },
                    params: {
                        expectedDescription: 'Повседневные товары, электроника и тысячи других товаров со скидками, акциями и' +
                            ' кешбэком баллами Плюса.',
                    },
                })
            ),
        }),

        makeSuite('Шапка.', {
            environment: 'testing',
            feature: 'Мой маркет',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            headerNav: () => this.createPageObject(Header2Nav),
                            profileMenu: () => this.createPageObject(Header2ProfileMenu),
                            address: () => this.createPageObject(HeaderRegionPopup),
                            headerMenu: () => this.createPageObject(Header2Menu),
                        });

                        await this.browser.setState(
                            'Tarantino.data.result',
                            [indexPageMock, navigationRootMock, navigationCatalogMock]
                        );
                        await this.browser.yaOpenPage('market:index');
                    },
                },
                {
                    'Авторизованный пользователь.': mergeSuites(
                        prepareSuite(Header2NavSuite, {
                            params: {
                                isAuthWithPlugin: true,
                            },
                        }),
                        prepareSuite(Header2ProfileMenuSuite, {
                            params: {
                                isAuthWithPlugin: true,
                            },
                        })
                    ),
                },
                prepareSuite(Header2AddressSuite)
            ),
        }),

        makeSuite('Cookie.', {
            story: mergeSuites(
                {
                    'Открытие страницы с параметром lr в url.': prepareSuite(CookiesLandingRegionSuite, {
                        hooks: {
                            async beforeEach() {
                                await this.browser.yaOpenPage('market:index');
                                await this.browser.deleteCookie('lr');
                                await this.browser.yaOpenPage('market:index', {lr: '213'});
                                return this.browser.yaOpenPage('market:index', {lr: '54'});
                            },

                            after() {
                                return this.browser.deleteCookie('lr');
                            },
                        },
                    }),
                },
                {
                    'Поисковой запрос с названием города.': prepareSuite(Search2LandingRegionCookieSuite, {
                        hooks: {
                            async beforeEach() {
                                await this.browser.allure.runStep(
                                    'Удаляем cookie lr',
                                    () => this.browser.deleteCookie('lr')
                                );
                                return this.browser.yaOpenPage('market:index');
                            },
                        },
                    }),
                }
            ),
        }),
        prepareSuite(verifiedCancellationPopup, {
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

        prepareSuite(cancellationRejectionPopup, {
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

        prepareSuite(deliveryReschedulePopupSuite, {
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

        prepareSuite(verifyDeliveryRescheduleSuite, {
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
                    return this.createPageObject(Header2);
                },
                popupModal() {
                    return this.createPageObject(Modal, {
                        root: `${Modal.root} [data-auto="yaPlusCashbackOnboarding"]`,
                    });
                },
            },
        }),

        prepareSuite(yandexHelpTooltip, {
            params: {
                isAuthWithPlugin: true,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.deleteCookie(COOKIE.YANDEX_HELP);
                    return this.browser.yaOpenPage('market:index');
                },
            },
        }),

        monobrand,
        banners,
        ugcPoll,
        cashbackDealTermSuite,
        cheapestAsGiftDealTermSuite,
        genericBundleDealTermSuite,
        distributionFooterDesktop,
        a11ySnippet
    ),
});
