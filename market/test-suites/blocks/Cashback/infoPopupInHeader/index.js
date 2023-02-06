import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// PageObject
import HeaderPlusBalance from '@self/root/src/widgets/content/header/PlusBalance/components/View/__pageObject';
import YaPlusPopupContent from '@self/root/src/components/YaPlusPopupContent/__pageObject';

import COOKIES from '@self/root/src/constants/cookie';

// suites
import infoPopupSuite from './suite';

export default makeSuite('Онбординг с информацией о плюсе.', {
    feature: 'Выгода Плюса',
    environment: 'kadavr',
    issue: 'MARKETFRONT-47463',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    headerPlusBalance: () => this.createPageObject(HeaderPlusBalance, {
                        parent: this.header2,
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
            meta: {
                id: 'marketfront-5180',
            },
            suiteName: 'Авторизованный плюсовик без куки и с балансом.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: true,
                isPopupCookieSet: false,
                hasYaPlusBalance: true,
            },
        }),
        prepareSuite(infoPopupSuite, {
            meta: {
                id: 'marketfront-5181',
            },
            suiteName: 'Авторизованный плюсовик без куки и без баланса.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: true,
                isPopupCookieSet: false,
                hasYaPlusBalance: false,
            },
        }),
        prepareSuite(infoPopupSuite, {
            meta: {
                id: 'marketfront-5182',
            },
            suiteName: 'Авторизванный плюсовик с кукой и с балансом.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: true,
                isPopupCookieSet: true,
                // с балансом или без флоу одинковый
                hasYaPlusBalance: true,
            },
        }),
        // end Блоки с плюсовиком
        // start Блоки с авторизованным неплюсовиком
        prepareSuite(infoPopupSuite, {
            meta: {
                id: 'marketfront-5183',
            },
            suiteName: 'Авторизованный неплюсовик без куки и с балансом.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: false,
                isPopupCookieSet: false,
                hasYaPlusBalance: true,
            },
        }),
        prepareSuite(infoPopupSuite, {
            meta: {
                id: 'marketfront-5184',
            },
            suiteName: 'Авторизованный неплюсовик без куки и без балансом.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: false,
                isPopupCookieSet: false,
                hasYaPlusBalance: false,
            },
        }),
        prepareSuite(infoPopupSuite, {
            meta: {
                id: 'marketfront-5185',
            },
            suiteName: 'Авторизванный неплюсовик с кукой и с балансом.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: false,
                isPopupCookieSet: true,
                hasYaPlusBalance: true,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизованный неплюсовик без куки и сгоранием кешбэка.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: false,
                isPopupCookieSet: false,
                hasYaPlusBalance: false,
                hasCashbackAnnihilation: true,
            },
        }),
        prepareSuite(infoPopupSuite, {
            suiteName: 'Авторизванный неплюсовик с кукой и сгоранием кешбэка.',
            params: {
                isAuthWithPlugin: true,
                hasYaPlus: false,
                isPopupCookieSet: true,
                hasYaPlusBalance: true,
                hasCashbackAnnihilation: true,
            },
        }),
        // end Блоки с авторизованным неплюсовиком
        prepareSuite(infoPopupSuite, {
            meta: {
                id: 'marketfront-5185',
            },
            suiteName: 'Не авторизванный пользователь.',
            params: {
                isAuthWithPlugin: false,
                id: 'marketfront-5186',
            },
        })
    ),
});
