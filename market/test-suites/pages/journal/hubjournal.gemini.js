import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import Main from '@self/platform/spec/page-objects/main';
import {hideHeadBanner, hideHeader, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import FeaturedPages from '@self/platform/spec/page-objects/Journal/FeaturedPages';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalHubJournal',
    url: '/journal/food',
    ignore: {every: Counter.root},
    selector: Main.root,
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
            before(actions) {
                // Вырезаем все сниппеты Журнала, потому что они плавают по высоте
                new ClientAction(actions).removeElems(FeaturedPages.journalEntrypoint);
            },
            capture() {
            },
        },
        {
            suiteName: 'FeaturedArticle',
            selector: `${FeaturedPages.root} ${FeaturedPages.journalEntrypoint}:nth-child(1)`,
            capture() {
            },
        },
        {
            suiteName: 'RegularArticle',
            selector: `${FeaturedPages.root} ${FeaturedPages.journalEntrypoint}:nth-child(2)`,
            capture() {
            },
        },
    ],
};
