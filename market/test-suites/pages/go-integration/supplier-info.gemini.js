import {setCookies} from '@yandex-market/gemini-extended-actions';

import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'Yandex Go - SupplierInfo',
    url: '/yandex-go/shops-jur-info?shopIds=431782&supplierIds=1183708',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
        ]);
    },
    childSuites: [
        MainSuite,
    ],
};
