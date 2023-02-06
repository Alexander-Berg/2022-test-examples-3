// pageObjects
import Mooa from '@self/platform/spec/page-objects/mooa';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import Paranja from '@self/platform/spec/page-objects/paranja';
import PersonalCabinetMenu from '@self/platform/spec/page-objects/components/PersonalCabinetMenu';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import UserVideoFooter from '@self/platform/components/UserVideo/Footer/__pageObject';
import UserVideos from '@self/platform/widgets/content/UserVideos/__pageObject';

import utils, {setCookies} from '@yandex-market/gemini-extended-actions/';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import cookies from '@self/platform/constants/cookie';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const MY_VIDEOS_URL = '/my/videos';

export default {
    suiteName: 'MyVideos',
    url: MY_VIDEOS_URL,
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
            suiteName: 'UserWithVideos',
            selector: [PersonalCabinetMenu.root, UserVideos.root],
            ignore: [
                {every: UserVideoFooter.viewsCount},
                {every: 'iframe'},
                {every: 'img'},
            ],
            before(actions) {
                utils.authorize.call(actions, {
                    login: 'test.author.cabinet',
                    password: '1221240tac',
                    url: MY_VIDEOS_URL,
                });

                const selector = [
                    ModalFloat.overlay,
                    `${Paranja.root}${Paranja.stateOpen}`,
                    RegionPopup.content,
                    Mooa.root,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(UserVideos.root, 5000);
            },
        },
        {
            suiteName: 'NoVideos',
            selector: [PersonalCabinetMenu.root, UserVideos.root],
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
                    url: MY_VIDEOS_URL,
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(UserVideos.root, 5000);
            },
        },
    ],
};
