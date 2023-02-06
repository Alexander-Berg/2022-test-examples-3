import ProductCardVendorPromoBadge from '@self/platform/spec/page-objects/ProductCardVendorPromoBadge';


import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'VendorPromoBadgeOnKM',
    url: '/product--yandex-stantsiia-umnaia-kolonka-dlia-umnogo-doma/1971204201',
    selector: ProductCardVendorPromoBadge.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    capture() {},
};
