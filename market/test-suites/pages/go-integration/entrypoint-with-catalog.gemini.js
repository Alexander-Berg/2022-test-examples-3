import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import utils from '@yandex-market/gemini-extended-actions/';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

import EntrypointCmsLayoutMock
    from '@self/root/src/spec/hermione/kadavr-mock/tarantino/YandexGoEntrypointWithCatalogPage';
import EntrypointCmsConfigMock
    from '@self/root/src/spec/hermione/kadavr-mock/tarantino/YandexGoEntrypointWithCatalogPage/config';
import {createNavigationTree} from '@self/root/src/spec/hermione/kadavr-mock/cataloger/expressNavigationTree';
import {minimalWarehouses} from '@self/root/src/spec/hermione/kadavr-mock/report/warehouses';

import ExpressCatalogEntrypointsSuite from '@self/platform/spec/gemini/test-suites/blocks/ExpressCatalogEntrypoints';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {ROOT_NID} from '@self/root/src/constants/categories';

const testUser = profiles.testachi;

export default {
    suiteName: 'Yandex Go - Entrypoint with catalog page [KADAVR]',
    url: '/blank',
    before(actions) {
        createSession.call(actions);

        setDefaultGeminiCookies(actions);

        setState.call(actions, 'Tarantino.data.result', [
            EntrypointCmsLayoutMock,
            EntrypointCmsConfigMock,
        ]);
        setState.call(actions, 'Cataloger.tree', createNavigationTree(ROOT_NID));
        setState.call(actions, 'report', minimalWarehouses);
        utils.authorize.call(actions, {
            login: testUser.login,
            password: testUser.password,
            url: '/yandex-go/entrypoint-with-catalog?lr=213&gps=37.560074%2C55.659576',
        });
    },
    after(actions) {
        deleteSession.call(actions);
    },
    childSuites: [
        ExpressCatalogEntrypointsSuite,
    ],
};
