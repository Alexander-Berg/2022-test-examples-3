import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import UgcScrollBoxSuite from '@self/platform/spec/gemini/test-suites/blocks/ugcscrollox.gemini';
import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import ReviewSnippet from '@self/platform/spec/page-objects/ReviewSnippet';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';

import {
    hideRegionPopup,
    hideMooa,
    hideModalFloat,
    hideGallerySlider,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'Departament',
    url: '/catalog/54440',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideMooa(actions);
        initLazyWidgets(actions, 6000);
    },
    childSuites: [
        {
            ...MainSuite,
            url: '/catalog--tsifrovye-tovary-i-podarochnye-sertifikaty/18059629',
            ignore: [
                {every: ProductSnippet.root},
                {every: ReviewSnippet.root},
                {every: '[data-zone-name="navnode"]'},
                {every: Counter.root},
            ],
            before(actions) {
                hideGallerySlider(actions);
                hideElementBySelector(actions, UgcScrollBoxSuite.selector);
            },
        },
        {
            suiteName: 'ScrollBoxProductSnippet',
            selector: `${ProductSnippet.root} > div`,
            ignore: {every: ProductSnippet.price},
            capture() {},
        },
        {
            suiteName: 'ScrollBoxReviewSnippet',
            selector: `${ReviewSnippet.root}  a`,
            capture() {},
        },
        UgcScrollBoxSuite,
        {
            suiteName: 'UgcSnippet',
            selector: `${UgcScrollBoxSuite.selector} [data-cs-name="navigate"]`,
            capture() {},
        },
    ],
};
