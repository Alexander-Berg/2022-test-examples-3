import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import MediaKitHeader from '@self/platform/spec/page-objects/Journal/MediaKitHeader';
import {
    hideMediaKitHeader,
} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'JournalMediaKit',
    url: '/mediakit-journal',
    before(actions) {
        setDefaultGeminiCookies(actions);
        disableAnimations(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideMediaKitHeader(actions);
            },
        },
        {
            suiteName: 'MediaKitHeader',
            selector: MediaKitHeader.root,
            capture() {},
        },
    ],
};
