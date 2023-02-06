import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

import TabsPopupShadowSuite from '@self/platform/spec/gemini/test-suites/blocks/HeaderTabs/tabsPopupShadow';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'WishlistEmpty',
    url: '/my/wishlist',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            suiteName: 'Authorized',
            selector: MainSuite.selector,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: '/my/wishlist',
                });
                MainSuite.before(actions);
            },
            capture() {
            },
            after(actions) {
                utils.logout.call(actions);
            },
        },
        {
            suiteName: 'Unauthorized',
            selector: MainSuite.selector,
            before(actions) {
                MainSuite.before(actions);
            },
            capture() {
            },
        },
        TabsPopupShadowSuite,
    ],
};
