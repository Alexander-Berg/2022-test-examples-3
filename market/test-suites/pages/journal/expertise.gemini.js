import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import Main from '@self/platform/spec/page-objects/main';
import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideHeadBanner,
    hideHeader,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalExpertise',
    url: '/journal/expertise/kak-izbezhat-ukusa-kleshha',
    ignore: {every: Counter.root},
    selector: Main.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        // Скрываем хэдэр, т.к. тень от него приводит к пиксельной тряске
        hideHeadBanner(actions);
        hideHeader(actions);
    },
    capture(actions) {
        actions.waitForElementToShow(Main.root, 10000);
    },
};
