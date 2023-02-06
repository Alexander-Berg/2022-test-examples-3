import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'UGC Landing Page',
    url: '/live-love-write',
    before(actions) {
        setDefaultGeminiCookies(actions);
        // Удаляем со страницы svg самолёта и воздушного шара во втором блоке. Они регулярно отрисовываются со смещением
        // в 1-2 пикселя, и роняют скринтест целиком
        hideElementBySelector(actions, '[data-autotest-id="2"] svg > g > g:nth-child(3)');
        hideElementBySelector(actions, '[data-autotest-id="2"] svg > g > g:nth-child(2)');
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: ScrollBox.root},
            ],
        },
    ],
};
