import utils from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Wishlist from '@self/project/src/widgets/content/Wishlist/__pageObject';

import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Wishlist',
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
            login: 'mrktcashback@yandex.ru',
            password: 'phrd11zc2',
            url: '/my/wishlist',
        });

        new ClientAction(actions).removeElems(selector);
    },
    childSuites: [
        {
            suiteName: 'Wishlist',
            selector: Wishlist.root,
            ignore: [
                {every: '[data-zone-name="offers-сounter"]'},
                {every: '[data-zone-name="reviews-count"]'},
            ],
            capture(actions) {
                actions.waitForElementToShow(Wishlist.root, 5000);
            },
        },
        {
            ...MainSuite,
            before(actions) {
                new ClientAction(actions).removeElems(Wishlist.root);
            },
        },
    ],
    after(actions) {
        utils.logout.call(actions);
    },
};
