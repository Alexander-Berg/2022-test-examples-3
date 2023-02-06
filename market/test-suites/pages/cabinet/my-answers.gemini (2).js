import utils, {setCookies} from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import UserAnswers from '@self/platform/spec/page-objects/widgets/content/UserAnswers';
import PersonalCabinetMenu from '@self/platform/spec/page-objects/components/PersonalCabinetMenu';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import cookies from '@self/platform/constants/cookie';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const ANSWERS_URL = '/my/answers';

export default {
    suiteName: 'MyAnswers',
    url: ANSWERS_URL,
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
            suiteName: 'UserWithAnswers',
            selector: [PersonalCabinetMenu.root, UserAnswers.root],
            ignore: {every: 'picture'},
            before(actions) {
                const selector = [
                    ModalFloat.overlay,
                    `${Paranja.root}${Paranja.stateOpen}`,
                    RegionPopup.content,
                    Mooa.root,
                ].join(', ');
                utils.authorize.call(actions, {
                    login: 'test.author.cabinet',
                    password: '1221240tac',
                    url: ANSWERS_URL,
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(UserAnswers.root, 5000);
            },
        },
        {
            suiteName: 'NoAnswers',
            selector: [PersonalCabinetMenu.root, UserAnswers.root],
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
                    url: ANSWERS_URL,
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(UserAnswers.root, 5000);
            },
        },
    ],
};
