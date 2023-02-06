import utils from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import ProductCompareLists from '@self/platform/spec/page-objects/ProductCompareLists';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A24%3A32.036829.png
    suiteName: 'CompareLists',
    url: '/compare-lists',
    selector: ProductCompareLists.root,
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
            url: '/compare-lists',
        });

        new ClientAction(actions).removeElems(selector);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    capture(actions) {
        actions.waitForElementToShow(ProductCompareLists.root, 5000);
    },
};
