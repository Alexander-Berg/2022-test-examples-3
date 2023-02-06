import Showcase from '@self/platform/spec/page-objects/Showcase';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Viewport from '@self/platform/spec/page-objects/Viewport';
import WysiwygText from '@self/platform/spec/page-objects/components/Wysiwyg';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import LogoCarousel from '@self/platform/spec/page-objects/widgets/parts/LogoCarousel';
import FeedBZV1Snippet from '@self/project/src/widgets/content/FeedBZV1/components/Snippet/__pageObject';
import FeedSnippet from '@self/platform/spec/page-objects/FeedSnippet';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

export default {
    suiteName: 'BrandsPay',
    url: '/brands/152718',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        disableAnimations(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A57%3A51.571396.png
            ...MainSuite,
            ignore: [
                {every: Showcase.snippet},
                // Карусель скидочных предложений
                `${ScrollBox.root} ${Viewport.root}`,
                LogoCarousel.root,
                // Последний абзац текст о бренде с переменным число товаров
                `${WysiwygText.root} p:last-child`,
                {every: FeedSnippet.root},
                {every: FeedBZV1Snippet.root},
            ],
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A59%3A12.681316.jpg
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
    ],
};
