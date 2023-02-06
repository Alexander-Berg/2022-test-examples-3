import utils from '@yandex-market/gemini-extended-actions';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';


import {
    hideRegionPopup,
    hideDevTools,
    hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';

import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'HubDepartament',
    url: '/catalog--kompiuternye-komplektuiushchie/54536',
    before(actions) {
        setDefaultGeminiCookies(actions);
        // Добавил авторизацию, чтобы появилась карусель "Популярные товары", а первый сниппет в ней меняется реже
        utils.authorize.call(actions, {
            login: profiles.emptyWishlist.login,
            password: profiles.emptyWishlist.password,
            url: '/catalog--kompiuternye-komplektuiushchie/54536',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 8000);
                hideDevTools(actions);
                hideRegionPopup(actions);
                hideHeadBanner(actions);
            },
        },
    ],
};
