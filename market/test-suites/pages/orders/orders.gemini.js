import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

import {hideRegionPopup, hideDevTools, hideFooter} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'MyOrders',
    url: '/my/orders',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideFooter(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            suiteName: 'Orders unauthorized',
        },
        {
            suiteName: 'Orders authorized',
            selector: MainSuite.selector,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.Recomend2017.login,
                    password: profiles.Recomend2017.password,
                    url: '/my/orders',
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {},
        },
        {
            suiteName: 'No orders',
            selector: MainSuite.selector,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: '/my/orders',
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {},
        },
    ],
};
