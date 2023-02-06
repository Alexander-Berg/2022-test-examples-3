import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import Snippet from '@self/platform/spec/page-objects/Snippet';
import DefaultOfferPrice from '@self/platform/spec/page-objects/components/DefaultOffer/DefaultOfferPrice';
import OfferSnippet from '@self/platform/spec/page-objects/b-offer-snippet';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'AdultCard',
    url: '/product--vodka-tsarskaia-originalnaia-0-5-l/270940092',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A05%3A05.041884.jpg
            ...MainSuite,
            ignore: [
                {every: Snippet.root},
                {every: DefaultOfferPrice.root},
                {every: OfferSnippet.price},
                {every: OfferSnippet.rating},
            ],
        },
    ],
};
