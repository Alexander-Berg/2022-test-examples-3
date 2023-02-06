import VendorProductLinePromo from '@self/platform/spec/page-objects/VendorProductLinePromo';

import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat, hideScrollbar} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'VendorProductLinePromoSmartPhones',
    url: '/catalog--smartfony/16814639/list',
    selector: VendorProductLinePromo.root,
    ignore: [
        {every: VendorProductLinePromo.price},
    ],
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        hideScrollbar(actions);
        // Дожидаемся загрузки карусели
        actions.waitForElementToShow(`${VendorProductLinePromo.root} ${VendorProductLinePromo.price}`, 3000);
    },
    capture() {},
};
