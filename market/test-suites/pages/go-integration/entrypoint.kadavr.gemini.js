import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import utils from '@yandex-market/gemini-extended-actions/';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import yandexGoEntrypointCmsMarkup
    from '@self/root/src/spec/hermione/kadavr-mock/tarantino/yandexGoEntrypoint';
import navigationTree from '@self/root/src/spec/hermione/kadavr-mock/cataloger/expressNavigationTree';
import {minimalWarehouses} from '@self/root/src/spec/hermione/kadavr-mock/report/warehouses';
import YandexGoCatalogSuite from '@self/platform/spec/gemini/test-suites/blocks/YandexGo/entypoint-catalog.gemini';

import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const testUser = profiles.testachi;

export default {
    suiteName: 'Yandex Go - Entrypoint[KADAVR]',
    url: '/blank',
    before(actions) {
        createSession.call(actions);

        setDefaultGeminiCookies(actions);

        setState.call(actions, 'report', minimalWarehouses);

        setState.call(actions, 'Tarantino.data.result', [
            yandexGoEntrypointCmsMarkup,
        ]);

        setState.call(actions, 'Cataloger.tree', navigationTree);

        utils.authorize.call(actions, {
            login: testUser.login,
            password: testUser.password,
            url: '/yandex-go/entrypoint?entrypoint=1&lr=213&gps=37.541773,55.749461',
        });
    },
    after(actions) {
        deleteSession.call(actions);
    },
    childSuites: [
        YandexGoCatalogSuite,
    ],
};
