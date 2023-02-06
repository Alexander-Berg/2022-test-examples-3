// suites
import OfferSummarySuite from '@self/platform/spec/gemini/test-suites/blocks/KO/OfferSummary';
import ProductStickerSuite from '@self/platform/spec/gemini/test-suites/blocks/n-product-sticker';
import OfferWarningsSuite from '@self/platform/spec/gemini/test-suites/blocks/OfferWarning';
import OfferPage from '@self/platform/spec/page-objects/widgets/pages/OfferPage';
// helpers
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {hideDevTools, hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {offers} from '@self/project/src/spec/gemini/configs/offers';
import cookies from '@self/root/src/constants/cookie';


const OFFER_ID = offers.offerAlco.wareid;

export default {
    suiteName: 'AlcoKo',
    url: `/offer/${OFFER_ID}`,
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'adult',
                value: '1:1:ADULT',
            },
            {
                // Алко-оффера остались только на CPC, скриншутим с выключенным режимом CPA-only
                name: cookies.HARD_CPA_ONLY_ENABLED,
                value: '0',
            },
        ]);
        hideDevTools(actions);
    },
    childSuites: [
        ProductStickerSuite,
        {
            ...OfferSummarySuite,
            selector: OfferPage.summary,
            before(actions) {
                hideProductTabs(actions);
                disableAnimations(actions);
            },
        },
        {
            ...OfferWarningsSuite,
            before(actions) {
                hideProductTabs(actions);
            },
        },
    ],
};
