import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// PageObjects
import WelcomeCashbackPromoBanner from '@self/root/src/components/WelcomeCashbackPromoBanner/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import WelcomeCashbackPopup from '@self/root/src/widgets/content/WelcomeCashbackPopup/components/View/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
// mocks
import {welcomeCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import {profiles} from '@self/project/src/spec/hermione/configs/profiles';
import {createWishlistState} from './fixtures/wishlist.mock';
// suites
import {bannerExistSuite, bannerNotExistSuite} from './welcomeCashbackBanner';

/**
 * Подготавлиавет стейт для вишлиста с offerCount офферами и перком WELCOME_CASHBACK
 * @param {number} offersCount - количество генерируемых офферов
 * @param {boolean} isPromoAvailable - доступен ли перк WELCOME_CASHBACK
 */
async function prepareState(offersCount, isPromoAvailable) {
    const {wishlistState, offerState} = createWishlistState(offersCount);
    const profile = profiles['pan-topinambur'];
    await this.browser.yaLogin(profile.login, profile.password);
    await this.browser.setState('Carter.items', []);
    await this.browser.setState('report', offerState);
    await this.browser.setState('persBasket', wishlistState);
    await this.browser.setState('Loyalty.collections.perks', isPromoAvailable ? [welcomeCashbackPerk] : []);
}

export default makeSuite('Плашка "Дарим 500 баллов".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-37781',
    feature: 'РК-500',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                welcomeCashbackPromoBanner: () => this.createPageObject(WelcomeCashbackPromoBanner),
                infoBannerButton: () => this.createPageObject(Button, {
                    parent: this.welcomeCashbackPromoBanner,
                }),
                welcomeCashbackPopupPortal: () => this.createPageObject(WelcomeCashbackPopup),
                welcomeCashbackPopup: () => this.createPageObject(PopupBase, {
                    root: this.welcomeCashbackPopupPortal,
                }),
            });
        },
        'Больше 10 итемов, пользователь без заказов.': prepareSuite(bannerExistSuite, {
            meta: {
                id: 'marketfront-4752',
            },
            hooks: {
                async beforeEach() {
                    const OFFERS_COUNT = 10;
                    await prepareState.call(this, OFFERS_COUNT, true);
                    return this.browser.yaOpenPage('market:wishlist');
                },
            },
        }),
        'Больше 10 итемов, пользователь с заказами.': prepareSuite(bannerNotExistSuite, {
            meta: {
                id: 'marketfront-4753',
            },
            hooks: {
                async beforeEach() {
                    const OFFERS_COUNT = 10;
                    await prepareState.call(this, OFFERS_COUNT, false);
                    return this.browser.yaOpenPage('market:wishlist');
                },
            },
        }),
        'Меньше 10 итемов.': prepareSuite(bannerNotExistSuite, {
            meta: {
                id: 'marketfront-4754',
            },
            hooks: {
                async beforeEach() {
                    const OFFERS_COUNT = 1;
                    await prepareState.call(this, OFFERS_COUNT, true);

                    return this.browser.yaOpenPage('market:wishlist');
                },
            },
        }),
    }),
});
