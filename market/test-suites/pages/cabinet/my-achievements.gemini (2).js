import utils, {setCookies} from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import Main from '@self/platform/spec/page-objects/main';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import cookies from '@self/platform/constants/cookie';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'MyAchievements',
    url: '/my/achievements',
    selector: Main.root,
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.LKOB_COOKIE,
                value: '1',
            },
        ]);
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');
        utils.authorize.call(actions, {
            login: profiles.reviewsfortest.login,
            password: profiles.reviewsfortest.password,
            url: '/my/achievements',
        });

        new ClientAction(actions).removeElems(selector);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    capture(actions) {
        actions.waitForElementToShow(Main.root, 5000);
    },
};
