import utils from '@yandex-market/gemini-extended-actions/';
import NewProductUgcVideoPage from '@self/platform/widgets/pages/NewProductUgcVideoPage/__pageObject';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const KM_VIDEO_ADD_URL = '/product--telefon-nokia-3310/160291/video/add';

export default {
    suiteName: 'KM-video-new',
    url: KM_VIDEO_ADD_URL,
    childSuites: [
        {
            suiteName: 'Authorized',
            selector: NewProductUgcVideoPage.root,
            before(actions) {
                utils.authorize.call(actions, {
                    login: 'test.author.cabinet',
                    password: '1221240tac',
                    url: KM_VIDEO_ADD_URL,
                });
                setDefaultGeminiCookies(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'Unauthorized',
            selector: NewProductUgcVideoPage.root,
            before(actions) {
                setDefaultGeminiCookies(actions);
            },
            capture() {
            },
        },
    ],
};
