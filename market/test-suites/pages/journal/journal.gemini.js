import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import Main from '@self/platform/spec/page-objects/main';
import FeaturedPages from '@self/platform/spec/page-objects/Journal/FeaturedPages';
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import {hideHeadBanner, hideHeader, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalMain',
    url: '/journal',
    selector: Main.root,
    ignore: {every: Counter.root},
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        // Скрываем хэдэр, т.к. тень от него приводит к пиксельной тряске
        hideHeadBanner(actions);
        hideHeader(actions);
    },
    childSuites: [
        {
            suiteName: 'Main',
            ignore: [
                {every: FeaturedPages.journalEntrypoint},
            ],
            capture() {
            },
        },
        {
            suiteName: 'ScrollBoxArticle',
            selector: `${ScrollBox.root} ${FeaturedPages.journalEntrypoint}`,
            capture() {
            },
        },
    ],
};
