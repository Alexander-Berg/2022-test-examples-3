import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';


export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A30%3A06.026359.png
    suiteName: 'AlphaLanding',
    url: '/promo/alfa-bank-credit-cards',
    ignore: 'iframe',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        disableAnimations(actions);
        actions.wait(3000); // Для того, чтобы форма окончательно догрузилась
    },
    childSuites: [MainSuite],
};
