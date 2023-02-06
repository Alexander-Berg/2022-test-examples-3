import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import FeedSnippet from '@self/platform/spec/page-objects/FeedSnippet';
import JournalScrollBox from '@self/platform/spec/gemini/test-suites/blocks/journal/scrollbox';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import utils from '@yandex-market/gemini-extended-actions';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const EXPIRED_OFFER_URL = '/offer/ASbTsGLUbvf3b9cSlMdv-g';

export default {
    suiteName: 'KO-Expired',
    url: EXPIRED_OFFER_URL,
    before(actions) {
        setDefaultGeminiCookies(actions);
        // Тут нужна для авторизация, потому что для незалогина страничка протухшего оффера - это просто 404.
        utils.authorize.call(actions, {
            login: profiles.Recomend2017.login,
            password: profiles.Recomend2017.password,
            url: EXPIRED_OFFER_URL,
        });
        MainSuite.before(actions);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: FeedSnippet.root},
                ScrollBox.root,
                // селектор для директа
                '[id*="yandex_adv"]',
            ],
        },
        {
            suiteName: 'PopularScrollBoxSnippet',
            selector: FeedSnippet.root,
            ignore: {every: ProductSnippet.price},
            capture() {},
        },
        JournalScrollBox,
    ],
};
