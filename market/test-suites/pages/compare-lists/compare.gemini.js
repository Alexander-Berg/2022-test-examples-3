import utils from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import ProductCompare from '@self/platform/spec/page-objects/ProductCompare';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A12%3A18.149191.png
    suiteName: 'Compare',
    url: {
        pathname: '/compare/3WoZXmerGt3vBYDwA8PJK91puM3w',
        query: {
            hid: 91491,
            id: [1831859610, 1722193751],
        },
    },
    selector: ProductCompare.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');
        utils.authorize.call(actions, {
            login: profiles.reviewsfortest.login,
            password: profiles.reviewsfortest.password,
            url: '/compare/3WoZXmerGt3vBYDwA8PJK91puM3w?hid=91491&id=1831859610&id=1722193751',
        });

        new ClientAction(actions).removeElems(selector);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    capture(actions) {
        actions.waitForElementToShow(ProductCompare.root, 5000);
    },
};
