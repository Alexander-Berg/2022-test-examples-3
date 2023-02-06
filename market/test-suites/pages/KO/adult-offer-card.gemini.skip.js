import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import deleteCookie from '@yandex-market/gemini-extended-actions/actions/deleteCookie';
import AgeConfirmation from '@self/platform/spec/page-objects/widgets/parts/AgeConfirmation';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {offers} from '@self/project/src/spec/gemini/configs/offers';

const OFFER_ID = offers.offerAlco.wareid;

export default {
    suiteName: 'AdultKOWarning',
    url: `/offer/${OFFER_ID}`,
    before(actions) {
        deleteCookie.call(actions, 'adult');
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            capture(actions) {
                // Ждём появления предупреждения - чтобы получить падение если оффер протухнет
                actions.waitForElementToShow(AgeConfirmation.root, 3000);
            },
        },
    ],
};
