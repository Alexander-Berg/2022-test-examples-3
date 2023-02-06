import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import PopularSuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/brands-popular';
import BrandGlossarySuite from '@self/platform/spec/gemini/test-suites/blocks/Brands/brand-glossary';
import BrandsPopular from '@self/platform/spec/page-objects/components/PopularBrands';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';


export default {
    suiteName: 'BrandsAll',
    url: '/brands',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                MainSuite.before(actions);
                const selector = [PopularSuite.selector, BrandGlossarySuite.selector].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        {
            ...PopularSuite,
            ignore: [
                {every: BrandsPopular.brands},
                {every: BrandsPopular.snippet},
            ],
        },
        BrandGlossarySuite,
    ],
};
