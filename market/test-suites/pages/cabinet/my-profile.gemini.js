import utils, {setCookies} from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import MyContentPage from '@self/platform/spec/page-objects/widgets/pages/MyContentPage';

import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import cookies from '@self/platform/constants/cookie';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const TASKS_URL = '/my/tasks';

export default {
    suiteName: 'MyProfile',
    url: TASKS_URL,
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.LKOB_COOKIE,
                value: '1',
            },
        ]);
    },
    childSuites: [
        {
            suiteName: 'EmptyCabinet',
            selector: MyContentPage.authorProfileSnippet,
            before(actions) {
                const selector = [
                    ModalFloat.overlay,
                    `${Paranja.root}${Paranja.stateOpen}`,
                    RegionPopup.content,
                    Mooa.root,
                ].join(', ');
                utils.authorize.call(actions, {
                    login: 'empty-wishlist-screenshooter',
                    password: 'empty-wishlist-screenshooter987',
                    url: TASKS_URL,
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(MyContentPage.authorProfileSnippet, 2000);
            },
        },
        {
            suiteName: 'CabinetWithContent',
            selector: MyContentPage.authorProfileSnippet,
            before(actions) {
                const selector = [
                    ModalFloat.overlay,
                    `${Paranja.root}${Paranja.stateOpen}`,
                    RegionPopup.content,
                    Mooa.root,
                ].join(', ');
                utils.authorize.call(actions, {
                    login: profiles.reviewsfortest.login,
                    password: profiles.reviewsfortest.password,
                    url: TASKS_URL,
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(MyContentPage.authorProfileSnippet, 2000);
            },
        },
    ],
};
