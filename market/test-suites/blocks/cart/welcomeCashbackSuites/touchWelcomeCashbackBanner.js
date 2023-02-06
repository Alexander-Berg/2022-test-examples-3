import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import COOKIE_NAME from '@self/root/src/constants/cookie';

import WelcomeCashbackPromoBanner from '@self/root/src/widgets/content/WelcomeCashbackPromoBanner/components/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

import {prepareCartState} from '@self/root/src/spec/hermione/fixtures/cart/cart';

import {bannerExistSuite, bannerNotExist} from './touchBannerSuites';

export default makeSuite('Баннер "Дарим 500 баллов Плюс."', {
    environment: 'kadavr',
    feature: 'РК-500',
    issue: 'MARKETFRONT-37784',
    story: mergeSuites({
        beforeEach() {
            this.setPageObjects({
                welcomeCashbackPromoBannerTouch: () => this.createPageObject(WelcomeCashbackPromoBanner),
                rulesLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackPromoBannerTouch,
                    root: '[data-auto="rulesLink"]',
                }),
                storeLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackPromoBannerTouch,
                    root: '[data-auto="storeLink"]',
                }),
                bannerCloseButton: () => this.createPageObject(Clickable, {
                    parent: this.welcomeCashbackPromoBannerTouch,
                }),
            });
        },
    },
    prepareSuite(bannerExistSuite, {
        suiteName: 'Акция доступна.',
        meta: {
            id: 'marketfront-4470',
        },
        params: {
            isAuthWithPlugin: true,
        },
        hooks: {
            async beforeEach() {
                await prepareCartState.call(this, {isPromoAvailable: true});
            },
        },
    }),
    prepareSuite(bannerNotExist, {
        suiteName: 'Акция не доступна.',
        meta: {
            id: 'marketfront-4801',
        },
        params: {
            isAuthWithPlugin: true,
        },
        hooks: {
            async beforeEach() {
                await prepareCartState.call(this, {isPromoAvailable: false});
            },
        },
    }),
    prepareSuite(bannerNotExist, {
        suiteName: 'Пользователь скрыл баннер',
        meta: {
            id: 'marketfront-4802',
        },
        params: {
            isAuthWithPlugin: true,
        },
        hooks: {
            async beforeEach() {
                await this.browser.yaSetCookie({
                    name: COOKIE_NAME.WELCOME_CASHBACK_TOUCH_SEARCH_BANNER_SHOW,
                    value: '1',
                });
                await prepareCartState.call(this, {isPromoAvailable: true});
            },
        },
    })),
});
