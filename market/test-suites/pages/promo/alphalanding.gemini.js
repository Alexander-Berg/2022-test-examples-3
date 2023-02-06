import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

export default {
    suiteName: 'AlphaCreditLanding',
    url: 'promo/alfa-bank-credit-cards',
    // Почему-то before с отключением анимации и установкой кук мог выполняться после MainSuite.before, а не до него
    // и тем самым флапать скринтест (появлялись баннер, шапка и проч.).
    childSuites: [
        {
            ...MainSuite,
            ignore: ['iframe'],
            before(actions) {
                setDefaultGeminiCookies(actions);
                disableAnimations(actions);
                MainSuite.before(actions);
            },
        },
    ],
};
