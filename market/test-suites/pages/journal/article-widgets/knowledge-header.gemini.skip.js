import KnowledgeHeader from '@self/platform/spec/page-objects/Journal/KnowledgeHeader';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KnowledgeHeader journal widget',
    selector: KnowledgeHeader.root,
    url: '/journal/knowledge/kak-vybrat-pylesos',
    ignore: {every: KnowledgeHeader.counter},
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    capture(actions) {
        actions.waitForElementToShow(KnowledgeHeader.root, 5000);
    },
};
