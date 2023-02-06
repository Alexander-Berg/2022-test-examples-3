import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KMTopOfferVendorFlag',
    url: '/product--smartfon-samsung-galaxy-a10/419572807?manufacturer_warranty=1',
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [{
        suiteName: 'KMTopOfferVendorFlag',
        selector: TopOfferSnippet.vendorFlag,
        before(actions) {
            hideProductTabs(actions);
        },
        capture() {},
    }],
};

