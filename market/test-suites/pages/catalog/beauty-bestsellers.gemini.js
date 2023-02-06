import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import SearchResultSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/offers-snippet.gemini';
import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import SearchHeader from '@self/platform/widgets/content/SearchHeader/redesign/__pageObject';


import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideAllElementsBySelector} from '@self/project/src/spec/gemini/helpers/hide';

const url = '/catalog--uvlazhnenie-i-pitanie-kozhi-litsa/17437152/list?hid=8476099';
const robotUrl = `${url}&_mod=robot`;

export default {
    suiteName: 'BeautyBestsellers',
    url,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A39%3A42.408376.png
        {
            ...MainSuite,
            url: robotUrl,
            ignore: [
                {every: ProductSnippet.root},
                SearchHeader.totalOffersCount,
            ],
            before(actions) {
                hideAllElementsBySelector(actions, [
                    SearchResultSuite.selector,
                ].join(', '));
            },
        },
        SearchResultSuite,
    ],
};
