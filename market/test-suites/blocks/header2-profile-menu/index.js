import url from 'url';
import {compose, prop} from 'lodash/fp';
import {makeSuite, makeCase, prepareSuite, mergeSuites} from 'ginny';

import Header2ProfileMenu from '@self/platform/spec/page-objects/header2-profile-menu';
import Modal from '@self/root/src/components/PopupBase/__pageObject';

import referralProgramMenuItem from '@self/root/src/spec/hermione/test-suites/desktop.blocks/referralProgramMenuItem';
import growingCashback from '@self/root/src/spec/hermione/test-suites/desktop.blocks/growingCashback/userMenuSuites';
import yaPlusOnboardingPopup from '@self/root/src/spec/hermione/test-suites/blocks/cashback/infoPopupInMenuItem';
import welcomeCashback from '@self/platform/spec/hermione/test-suites/blocks/WelcomeCashbackSuite/userMenu';
import welcomeCashbackPopup from '@self/platform/spec/hermione/test-suites/blocks/WelcomeCashbackSuite/welcomeCashbackPopup';
import yandexHelpMenuItem from '@self/root/src/spec/hermione/test-suites/blocks/yandexHelp/yandexHelpMenuItem';
import vacanciesLinkSuite from '@self/root/src/spec/hermione/test-suites/blocks/vacanciesLink/vacanciesLinkSuite.js';

const getQueryParamsFromUrl = compose(prop('query'), url.parse);

/**
 * Тесты на блок header2-profile-menu
 * @param {PageObject.Header2ProfileMenu} profileMenu
 */

export default makeSuite('Боковое меню пользователя.', {
    environment: 'kadavr',
    issue: 'MARKETVERSTKA-25318',
    id: 'marketfront-1109',
    story: mergeSuites({

        beforeEach() {
            return this.headerNav.clickOpen();
        },

        'Пункт меню "Заказы".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Заказы"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:orders', {
                            track: 'menu',
                        });
                        const actualPath = await this.profileMenu.getOrdersUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Купоны".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Купоны"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:bonus', {
                            track: 'menu',
                        });
                        const actualPath = await this.profileMenu.getBonusesUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Избранное".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Избранное"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:wishlist', {
                            track: 'menu',
                        });
                        const actualPath = await this.profileMenu.getWishlistUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Мои публикации".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Мои публикации"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:my-tasks', {
                            track: 'menu',
                        });
                        const actualPath = await this.profileMenu.getReviewsUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Списки сравнения".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Сравнение товаров"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:compare', {
                            track: 'menu',
                        });
                        const actualPath = await this.profileMenu.getCompareUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Трансляции".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Трансляции"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:live', {
                            track: 'menu',
                        });
                        const actualPath = await this.profileMenu.getLiveUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Настройки Маркета".': {
            'По умолчанию': {
                'содержит ссылку на страницу "Настройки"': makeCase({
                    async test() {
                        const expectedPath = await this.browser.yaBuildURL('market:my-settings');
                        const actualPath = await this.profileMenu.getSettingsUrl();

                        return this.expect(actualPath).to.be.link(
                            expectedPath,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },

        'Пункт меню "Выход".': {
            'По умолчанию': {
                'содержит ссылку с retpath равным текущему адресу': makeCase({
                    id: 'marketfront-889',
                    issue: 'MARKETVERSTKA-25704',
                    async test() {
                        const exitUrlRaw = await this.profileMenu.getExitUrl();
                        const currentUrl = await this.browser.getUrl();
                        const {retpath} = getQueryParamsFromUrl(exitUrlRaw);

                        await this.expect(retpath).to.be.link(currentUrl);
                    },
                }),
            },
        },
    },
    prepareSuite(vacanciesLinkSuite, {
        meta: {
            id: 'marketfront-4708',
            issue: 'MARKETFRONT-42335',
        },
        pageObjects: {
            parent() {
                return this.createPageObject(Header2ProfileMenu);
            },
        },
    }),
    prepareSuite(welcomeCashback),
    prepareSuite(welcomeCashbackPopup),
    prepareSuite(referralProgramMenuItem),
    prepareSuite(yandexHelpMenuItem),
    prepareSuite(growingCashback(async function prepareState() {
        return this.headerNav.clickOpen();
    })),
    prepareSuite(yaPlusOnboardingPopup(async function prepareState() {
        return this.headerNav.clickOpen()
            .then(() => this.headerNav.waitForMenuVisible());
    }), {
        pageObjects: {
            popupModal() {
                return this.createPageObject(Modal, {
                    root: `${Modal.root} [data-auto="yaPlusCashbackOnboarding"]`,
                });
            },
        },
    })
    ),
});
