import utils, {setCookies} from '@yandex-market/gemini-extended-actions';
import {setState, createSession} from '@yandex-market/kadavr/plugin/gemini';

import COOKIES from '@self/root/src/constants/cookie';

import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideModalFloat, hideParanja, hideRegionPopup, hideScrollbar} from '@self/platform/spec/gemini/helpers/hide';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export const commonAfterActions = (actions, schema) => {
    setState.call(actions, 'schema', schema);
    setCookies.setCookies.call(actions, [
        ...DEFAULT_COOKIES,
        // обязательно должна присутствовать кука ugcp
        {
            name: COOKIES.POLL_COOKIE,
            value: '1',
        },
    ]);
    hideRegionPopup(actions);
    hideModalFloat(actions);
    hideParanja(actions);
    hideScrollbar(actions);
};

export const commonBeforeActions = (actions, url) => {
    utils.authorize.call(actions, {
        login: profiles.ugctest3.login,
        password: profiles.ugctest3.password,
        url,
    });
    createSession.call(actions);
};

