import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalKnowledgeArticle',
    url: '/journal/knowledge/kak-vybrat-matras',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: Counter.root},
                {every: `${ScrollBox.root} picture`},
                {every: Votes.root},
            ],
        },
    ],
};
