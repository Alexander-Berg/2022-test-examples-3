import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Main from '@self/platform/spec/page-objects/main';
import VideoFrame from '@self/platform/spec/page-objects/widgets/parts/VideoFrame';
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
    suiteName: 'JournalRecipe',
    url: '/journal/recipe/stejki-iz-svininy',
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
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: Counter.root},
                VideoFrame.root,
            ],
            capture(actions) {
                actions.waitForElementToShow(Main.root, 10000);
            },
        },
    ],
};
