import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import CompareRow from '@self/platform/widgets/content/compare/Content/CompareRow/__pageObject';
import CompareHead from '@self/platform/widgets/content/compare/Content/CompareHead/__pageObject';
import {hideRegionPopup, hideDevTools, hideFooter, hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'Compare',
    url: '/compare',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideFooter(actions);
        hideElementBySelector(actions, CompareHead.root);
        utils.authorize.call(actions, {
            login: profiles.Recomend2017.login,
            password: profiles.Recomend2017.password,
            url: '/compare',
        });
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                CompareRow.priceRow,
            ],
            before(actions) {
                MainSuite.before(actions);
            },
        },
    ],
};
