import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import KMPopupCard from '@self/platform/spec/gemini/test-suites/blocks/KM/OfferInfoCard';
import KMPopupAction from '@self/platform/spec/gemini/test-suites/blocks/KM/OfferInfoAction';
import KMPopupContent from '@self/platform/spec/gemini/test-suites/blocks/KM/OfferInfoContent';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KM-Popup',
    url: '/product--red-dead-redemption-2/91708352/offers',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        KMPopupCard,
        KMPopupAction,
        KMPopupContent,
    ],
};
