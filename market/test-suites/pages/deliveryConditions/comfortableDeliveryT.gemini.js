import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideModalFloat, hideMooa, hideParanja, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A09%3A12.135607.png
    suiteName: 'ComfortableDeliveryT',
    url: '/my/order/conditions?_filter=%2Fcontent%2Fcontent%2Fcontent%2F1%2F73692774-ComfortableDelivery',
    selector: MainSuite.selector,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    capture() {},
};
