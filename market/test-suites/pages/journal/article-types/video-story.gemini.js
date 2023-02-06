import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import VideoFrame from '@self/platform/spec/page-objects/VideoFrame';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalVideoStory',
    url: '/journal/howto/gollivudskij-konturing-ot-Iry-Blan',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                VideoFrame.root,
                {every: Counter.root},
            ],
        },
    ],
};
