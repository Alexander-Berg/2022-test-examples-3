import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const KM_VIDEO_ADD_URL = '/product--telefon-nokia-3310/160291/video/add';

export default {
    suiteName: 'KM-video-add',
    url: KM_VIDEO_ADD_URL,
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.authorCabinet.login,
                    password: profiles.authorCabinet.password,
                    url: KM_VIDEO_ADD_URL,
                });
                setDefaultGeminiCookies(actions);
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
    ],
};
