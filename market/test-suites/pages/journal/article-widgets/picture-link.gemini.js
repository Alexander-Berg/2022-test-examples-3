import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import DefaultCard from '@self/platform/spec/page-objects/components/Journal/PictureLink/DefaultCard';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'PictureLink journal widget',
    url: '/journal/story/test-suites-picture-links',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        actions.waitForElementToShow(DefaultCard.root, 5000);
    },
    childSuites: [
        MainSuite,
    ],
};
