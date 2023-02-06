import utils from '@yandex-market/gemini-extended-actions/';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import ProductCompareLists from '@self/platform/spec/page-objects/ProductCompareLists';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A33%3A08.375417.jpg - незалогин
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A34%3A22.606712.jpg - залогин
    suiteName: 'CompareListsEmpty',
    url: '/compare-lists',
    before(actions) {
        setDefaultGeminiCookies(actions);
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');
        utils.authorize.call(actions, {
            login: 'testemptyrewies',
            password: 'testemptyrewies1',
            url: '/compare-lists',
        });
        new ClientAction(actions).removeElems(selector);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                actions.waitForElementToShow(ProductCompareLists.root, 5000);
            },
        },
    ],
    after(actions) {
        utils.logout.call(actions);
    },
};
