import utils from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'WishlistEmpty',
    url: '/my/wishlist',
    before(actions) {
        setDefaultGeminiCookies(actions);
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');
        utils.authorize.call(actions, {
            login: 'empty-wishlist-screenshooter',
            password: 'empty-wishlist-screenshooter987',
            url: '/my/wishlist',
        });
        new ClientAction(actions).removeElems(selector);
    },
    childSuites: [
        MainSuite,
    ],
    after(actions) {
        utils.logout.call(actions);
    },
};
