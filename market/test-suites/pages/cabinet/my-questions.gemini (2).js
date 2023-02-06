import utils, {setCookies} from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import UserQuestions from '@self/platform/spec/page-objects/widgets/content/UserQuestions';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import PersonalCabinetMenu from '@self/platform/spec/page-objects/components/PersonalCabinetMenu';
import cookies from '@self/platform/constants/cookie';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'MyQuestions',
    url: '/my/questions',
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
            suiteName: 'UserWithQuestions',
            selector: [PersonalCabinetMenu.root, UserQuestions.root],
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
                    url: '/my/questions',
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(UserQuestions.root, 5000);
            },
        },
        {
            suiteName: 'NoQuestions',
            selector: [PersonalCabinetMenu.root, UserQuestions.root],
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
                    url: '/my/questions',
                });

                new ClientAction(actions).removeElems(selector);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture(actions) {
                actions.waitForElementToShow(UserQuestions.root, 5000);
            },
        },
    ],
};
