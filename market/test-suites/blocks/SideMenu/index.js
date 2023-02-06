import {makeSuite, makeCase, prepareSuite, mergeSuites} from 'ginny';

import PopupSlider from '@self/root/src/components/PopupSlider/__pageObject';
import MenuCatalog from '@self/platform/spec/page-objects/components/MenuCatalog';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import AuthUserInfo from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/AuthUserInfo';
import NotAuthUserInfo from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/NotAuthUserInfo';
import WelcomeCashback from '@self/platform/spec/hermione/test-suites/blocks/WelcomeCashbackSuites/userMenu';
import growingCashback from '@self/root/src/spec/hermione/test-suites/touch.blocks/growingCashback/userMenuSuites';
import yandexHelpMenuItem from '@self/root/src/spec/hermione/test-suites/blocks/yandexHelp/yandexHelpMenuItem';
import yaPlusOnboardingPopup from '@self/root/src/spec/hermione/test-suites/blocks/cashback/infoPopupInMenuItem';

/**
 * Тесты на блок SideMenu.
 * @param {PageObject.SideMenu} sideMenu
 */
export default makeSuite('Блок бокового меню.', {
    story: mergeSuites({
        beforeEach() {
            this.setPageObjects({
                header: () => this.createPageObject(Header),
            });
        },

        'Пункт меню "Все категории".': {
            'При нажатии': {
                beforeEach() {
                    this.setPageObjects({
                        menuCatalog: () => this.createPageObject(MenuCatalog),
                    });

                    return this.header.clickMenuTrigger()
                        .then(() => this.sideMenu.waitForVisible());
                },

                'должен открывать панель с каталогом': makeCase({
                    id: 'm-touch-2100',
                    test() {
                        return this.sideMenu.clickCatalogItem()
                            .then(() => this.menuCatalog.isOpen())
                            .should.eventually.be.equal(true, 'Панель с каталогом отображается')

                            .then(() => this.browser.waitForVisible(SideMenu.root, 1000, true))
                            .should.eventually.be.equal(true, 'Боковое меню не отображается');
                    },
                }),
            },
        },

        'Пункт меню "Выйти".': {
            'Для авторизованного пользователя': {
                beforeEach() {
                    return this.browser.yaTestLogin()
                        .then(() => this.header.clickMenuTrigger())
                        .then(() => this.sideMenu.waitForVisible());
                },

                afterEach() {
                    return this.browser.yaLogout();
                },

                'должен быть виден': makeCase({
                    id: 'm-touch-2090',
                    test() {
                        return this.sideMenu.isVisible(this.sideMenu.exit)
                            .should.eventually.be.equal(true, 'Пункт меню отображается');
                    },
                }),

                'содержит ссылку для разлогина': makeCase({
                    id: 'm-touch-2090',
                    test() {
                        return this.sideMenu.getExitUrl()
                            .should.eventually.be.link({
                                hostname: '^passport',
                                query: {
                                    mode: 'logout',
                                    retpath: '.+',
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipPathname: true,
                            });
                    },
                }),
            },
        },

        'Блок с информацией пользователя.': {
            beforeEach() {
                this.setPageObjects({
                    authUserInfo: () => this.createPageObject(AuthUserInfo),
                    notAuthUserInfo: () => this.createPageObject(NotAuthUserInfo),
                });
            },

            'Для авторизованного пользователя': {
                beforeEach() {
                    return this.browser.yaTestLogin()
                        .then(() => this.header.clickMenuTrigger())
                        .then(() => this.sideMenu.waitForVisible());
                },

                afterEach() {
                    return this.browser.yaLogout();
                },

                'отображает аватар и данные пользователя': makeCase({
                    id: 'm-touch-2089',
                    test() {
                        return this.authUserInfo.isAvatarVisible()
                            .should.eventually.be.equal(true, 'Аватар отображается')

                            .then(() => this.authUserInfo.isInfoVisible())
                            .should.eventually.be.equal(true, 'Инфо отображается');
                    },
                }),
            },
        },

        // ВАЖНО! Данному сьюту по факту не обязательно открывать саму панель.
        // Хоть она и не видна, но ссылки мы можем протестировать и так.
        // Для открытия панели есть отдельный тест
        'По умолчанию': {
            'пункт меню "Регион" содержит ссылку на страницу': makeCase({
                id: 'm-touch-1898',
                test() {
                    return this.sideMenu.getRegionUrl()
                        .should.eventually.be.link({
                            pathname: '/my/region',
                            query: {
                                retPath: '.+', // пока просто проверим что там что-то есть
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
    prepareSuite(WelcomeCashback),
    prepareSuite(growingCashback(async function prepareState() {
        return this.header.clickMenuTrigger()
            .then(() => this.sideMenu.waitForVisible());
    })),
    prepareSuite(yandexHelpMenuItem, {
        hooks: {
            async beforeEach() {
                this.setPageObjects({
                    header: () => this.createPageObject(Header),
                });
                await this.browser.yaTestLogin();
                return this.header.clickMenuTrigger()
                    .then(() => this.sideMenu.waitForVisible());
            },
        },
    }),
    prepareSuite(yaPlusOnboardingPopup(async function prepareState() {
        return this.header.clickMenuTrigger()
            .then(() => this.sideMenu.waitForVisible());
    }), {
        pageObjects: {
            popupModal() {
                return this.createPageObject(PopupSlider);
            },
        },
    })
    ),
});
