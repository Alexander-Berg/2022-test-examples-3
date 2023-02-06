// файл переименован, чтобы исключить из прогона

import VendorPromoBadge from '@self/platform/spec/page-objects/VendorPromoBadge';

import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'ModelCardOfferVendorPromoBadge',
    url: '/product--smartfon-samsung-galaxy-a10/419572807',
    skip: true,
    selector: VendorPromoBadge.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    capture() {},
};
