import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';

import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'KMBook',
    // КМ на которых давно живут оффера
    // https://yql.yandex-team.ru/Operations/Yd6uGy3DcAOgZy8XPZ445J3O0etuZdtZw6BwWsgQdOo=
    url: '/product--khanauer-dzheims-mify-i-legendy-sviatoi-zemli/3884171',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        initLazyWidgets(actions, 5000);
    },
    childSuites: [
        MainSuite,
    ],
};
