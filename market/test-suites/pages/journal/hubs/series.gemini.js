import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalSeries',
    url: '/journal/series/sekrety-shef-povara-Alekseja-Zimina',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [{every: Counter.root}],
        },
    ],
};
