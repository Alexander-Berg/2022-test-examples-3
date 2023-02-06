import Main from '@self/platform/spec/page-objects/main';
import {hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'AnswerPage',
    selector: Main.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A34%3A58.736383.jpg
            suiteName: 'User',
            url: '/product-question-answer/1520271',
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A35%3A18.647588.jpg
            suiteName: 'Vendor',
            url: '/product-question-answer/1531153',
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A34%3A37.909277.png
            suiteName: 'Shop',
            url: '/product-question-answer/1668441',
            capture() {
            },
        },
    ],
};
