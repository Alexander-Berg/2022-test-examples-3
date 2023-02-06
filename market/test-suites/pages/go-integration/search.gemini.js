import {setCookies} from '@yandex-market/gemini-extended-actions';

import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'Yandex Go - Search',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
        ]);
    },
    childSuites: [
        {
            suiteName: 'TextSearch',
            url: '/yandex-go/search?text=iphone%2011',
            childSuites: [
                MainSuite,
            ],
        },
        {
            suiteName: 'CatalogSearch',
            url: '/yandex-go/search?hid=91491&nid=54726',
            childSuites: [
                MainSuite,
            ],
        },
    ],
};
