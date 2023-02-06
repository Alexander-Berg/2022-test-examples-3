import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {WELCOME_CASHBACK_ON_CART_SHOW} from '@self/root/src/constants/cookie';
import {welcomeCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

import WelcomeCashbackTooltip
    from '@self/root/src/widgets/content/WelcomeCashbackOnboardingTooltip/components/View/__pageObject';
import Title from '@self/root/src/uikit/components/Title/__pageObject';
import WelcomeCashbackPopup from '@self/root/src/widgets/content/WelcomeCashbackPopup/components/View/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

import {tooltipExistSuite, tooltipNotExistSuite} from './tooltipSuites';

export default makeSuite('Тултип "Получите 500 баллов Плюса".', {
    environment: 'kadavr',
    feature: 'РК-500',
    issue: 'MARKETFRONT-37782',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    welcomeCashbackOnboardingTooltip: () => this.createPageObject(WelcomeCashbackTooltip),
                    tooltipTitle: () => this.createPageObject(Title, {
                        parent: this.welcomeCashbackOnboardingTooltip,
                    }),
                    welcomeCashbackPopupPortal: () => this.createPageObject(WelcomeCashbackPopup),
                    welcomeCashbackPopup: () => this.createPageObject(PopupBase, {
                        root: this.welcomeCashbackPopupPortal,
                    }),
                    popupClickableElem: () => this.createPageObject(Clickable, {
                        parent: this.welcomeCashbackOnboardingTooltip,
                    }),
                });
            },
            async afterEach() {
                await this.browser.deleteCookie(WELCOME_CASHBACK_ON_CART_SHOW);
            },
        },
        prepareSuite(tooltipExistSuite, {
            suiteName: 'Авторизованный пользователь, без заказов.',
            params: {
                isAuthWithPlugin: true,
            },
            meta: {
                id: 'marketfront-4755',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Loyalty.collections.perks', [welcomeCashbackPerk]);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
                },
            },
        }),
        prepareSuite(tooltipNotExistSuite, {
            suiteName: 'Авторизованный пользователь, c заказами.',
            params: {
                isAuthWithPlugin: true,
            },
            meta: {
                id: 'marketfront-4756',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
                },
            },
        }),
        prepareSuite(tooltipExistSuite, {
            suiteName: 'Неавторизованный пользователь. Акция доступна.',
            params: {
                isAuthWithPlugin: false,
            },
            meta: {
                id: 'marketfront-4757',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Loyalty.collections.perks', [welcomeCashbackPerk]);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
                },
            },
        }),
        prepareSuite(tooltipNotExistSuite, {
            suiteName: 'Неавторизованный пользователь. Акция не доступна.',
            params: {
                isAuthWithPlugin: false,
            },
            meta: {
                id: 'marketfront-4758',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
                },
            },
        })
    ),
});
