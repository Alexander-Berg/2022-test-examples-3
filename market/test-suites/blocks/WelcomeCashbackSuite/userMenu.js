import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// pageObject
import AppPromoOnboardingMenuItem from '@self/root/src/components/AppPromoOnboardingMenuItem/__pageObject';
import SpecialMenuItem from '@self/root/src/components/SpecialMenuItem/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import WelcomeCashbackPopup from '@self/root/src/widgets/content/WelcomeCashbackPopup/components/View/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import {welcomeCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

import {bannerExistSuite, bannerNotExistSuite} from './userMenuSuites';

async function prepareState({isPromoAvailable}) {
    await this.browser.setState('Loyalty.collections.perks', isPromoAvailable ? [welcomeCashbackPerk] : []);
    await this.browser.yaOpenPage('market:index');
    return this.headerNav.clickOpen()
        .then(() => this.headerNav.waitForMenuVisible());
}

export default makeSuite('Пункт меню "500 баллов за Плюса".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-37778',
    feature: 'РК-500',
    story: mergeSuites({
        beforeEach() {
            this.setPageObjects({
                welcomeCashbackMenuItem: () => this.createPageObject(SpecialMenuItem, {
                    root: AppPromoOnboardingMenuItem.root,
                }),
                menuItemLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackMenuItem,
                }),
                welcomeCashbackPopup: () => this.createPageObject(PopupBase, {
                    root: WelcomeCashbackPopup.root,
                }),
            });
        },
        'Акция доступна': prepareSuite(bannerExistSuite, {
            meta: {
                id: 'marketfront-4458',
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {isPromoAvailable: true});
                },
            },
        }),
        'Акция не доступна': prepareSuite(bannerNotExistSuite, {
            meta: {
                id: 'marketfront-4765',
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {isPromoAvailable: false});
                },
            },
        }),
    }),
});
