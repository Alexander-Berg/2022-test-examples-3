import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// PageObject
import YaPlusMenuItem from '@self/root/src/components/YaPlusMenuItem/__pageObject';
import SpecialMenuItem from '@self/root/src/components/SpecialMenuItem/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import YaPlusPopupContent from '@self/root/src/components/YaPlusPopupContent/__pageObject';

import COOKIES from '@self/root/src/constants/cookie';

// suites
import infoPopupSuite from './suite';

export default prepareState => makeSuite('Онбординг с информацией о плюсе.', {
    feature: 'Выгода Плюса',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    yaPlusMenuItem: () => this.createPageObject(SpecialMenuItem, {
                        root: YaPlusMenuItem.root,
                    }),
                    menuItemLink: () => this.createPageObject(Link, {
                        parent: this.yaPlusMenuItem,
                    }),
                    yaPlusContent: () => this.createPageObject(YaPlusPopupContent, {
                        parent: this.popupModal,
                    }),
                });
                await this.browser.deleteCookie(COOKIES.YA_PLUS_ONBOARDING);
            },
        },
        // start Блоки с плюсовиком
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизованный плюсовик без куки и с балансом.',
            params: {
                hasYaPlus: true,
                isPopupCookieSet: false,
                hasYaPlusBalance: true,
                prepareState,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизованный плюсовик без куки и без баланса.',
            params: {
                hasYaPlus: true,
                isPopupCookieSet: false,
                hasYaPlusBalance: false,
                prepareState,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизванный плюсовик с кукой и с балансом.',
            params: {
                hasYaPlus: true,
                isPopupCookieSet: true,
                // с балансом или без флоу одинковый
                hasYaPlusBalance: true,
                prepareState,
            },
        }),
        // end Блоки с плюсовиком
        // start Блоки с авторизованным неплюсовиком
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизованный неплюсовик без куки и с балансом.',
            params: {
                hasYaPlus: false,
                isPopupCookieSet: false,
                hasYaPlusBalance: true,
                prepareState,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизованный неплюсовик без куки и без балансом.',
            params: {
                hasYaPlus: false,
                isPopupCookieSet: false,
                hasYaPlusBalance: false,
                prepareState,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизванный неплюсовик с кукой и с балансом.',
            params: {
                hasYaPlus: false,
                isPopupCookieSet: true,
                hasYaPlusBalance: true,
                prepareState,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизованный неплюсовик без куки и сгоранием кешбэка.',
            params: {
                hasYaPlus: false,
                isPopupCookieSet: false,
                hasYaPlusBalance: false,
                hasCashbackAnnihilation: true,
                prepareState,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизванный неплюсовик с кукой и сгоранием кешбэка.',
            params: {
                hasYaPlus: false,
                isPopupCookieSet: true,
                hasYaPlusBalance: true,
                hasCashbackAnnihilation: true,
                prepareState,
            },
        })
        // end Блоки с авторизованным неплюсовиком
    ),
});
