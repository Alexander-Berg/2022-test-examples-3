import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';


export default {
    suiteName: 'MyOrders',
    url: '/my/orders',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
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
                    login: profiles.ugctest3.login,
                    password: profiles.ugctest3.password,
                    url: '/my/orders',
                });
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
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {},
        },
    ],
};
