import Showcase from '@self/platform/spec/page-objects/Showcase';
import FeedSnippet from '@self/platform/spec/page-objects/FeedSnippet';
import WysiwygText from '@self/platform/spec/page-objects/components/Wysiwyg';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import LogoCarousel from '@self/platform/spec/page-objects/widgets/parts/LogoCarousel';
import FeedBZV1Snippet from '@self/project/src/widgets/content/FeedBZV1/components/Snippet/__pageObject';
import ScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import Viewport from '@self/root/src/components/Viewport/__pageObject';

import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideAllElementsBySelector} from '@self/project/src/spec/gemini/helpers/hide';


export default {
    suiteName: 'BrandsFree',
    url: '/brands/153043',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A56%3A12.358369.png
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, LogoCarousel.root);
                hideAllElementsBySelector(actions, FeedSnippet.root);
                hideAllElementsBySelector(actions, FeedBZV1Snippet.root);
            },
            ignore: [
                {every: Showcase.snippet},
                // Карусель скидочных предложений
                `${ScrollBox.root} ${Viewport.root}`,
                // Последний абзац текст о бренде с переменным число товаров
                `${WysiwygText.root} p:last-child`,
            ],
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A51%3A34.533335.jpg
            suiteName: 'CategorySnippet',
            selector: Showcase.snippet,
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A53%3A13.790035.jpg
            suiteName: 'FeedSnippet',
            selector: [
                FeedSnippet.root,
                FeedBZV1Snippet.root,
            ].join(', '),
            ignore: [
                FeedSnippet.price,
                FeedBZV1Snippet.price,
            ],
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A52%3A31.295182.jpg
            suiteName: 'DiscountSnippet',
            // Верхний вьюпорт - это вся карусель, следующий - отдельный сниппет этой карусели.
            selector: `${ScrollBox.root} ${Viewport.root} ${Viewport.root}`,
            ignore: [
                {every: 'svg'},
                {every: 'span'},
            ],
            capture() {
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A53%3A46.453454.jpg
            suiteName: 'LogoCarousel',
            selector: LogoCarousel.root,
            capture() {
            },
        },
    ],
};
