import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {
    hideRegionPopup, hideModalFloat, hideParanja, hideMooa,
} from '@self/platform/spec/gemini/helpers/hide';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A35%3A15.829506.jpg
    suiteName: 'DeliveryCalculator',
    url: '/my/order/conditions?_filter=%2Fcontent%2Fcontent%2Fcontent%2F0%2F68044644-DeliveryCalculator',
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
