import utils from '@yandex-market/gemini-extended-actions/';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Bonus',
    url: '/bonus',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A50%3A51.066888.jpg
            ...MainSuite,
            suiteName: 'Bonus unauthorized',
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A50%3A31.738520.jpg
            suiteName: 'Bonus authorized',
            selector: MainSuite.selector,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.ugctest3.login,
                    password: profiles.ugctest3.password,
                    url: '/bonus',
                });
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {},
        },
    ],
};
