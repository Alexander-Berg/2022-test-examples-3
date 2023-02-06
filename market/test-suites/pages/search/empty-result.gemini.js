import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideScrollbar,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ZeroStateCard from '@self/project/src/components/ZeroStateCard/__pageObject';

export default {
    suiteName: 'SearchPageEmptyResult [KADAVR]',
    // специально плохой текст
    url: '/search?text=mmmmmmmmmmmmmmmmmmmmmmmmmmm',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        hideScrollbar(actions);
    },
    childSuites: [
        {
            suiteName: 'ZeroStateCard',
            selector: `${ZeroStateCard.root} > div`,
            capture() {},
        },
    ],
};
