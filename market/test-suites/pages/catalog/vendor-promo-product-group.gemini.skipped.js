import VendorPromoProductGroup from '@self/platform/spec/page-objects/widgets/content/VendorPromoProductGroup';

import {hideModalFloat, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'VendorPromoProductGroup',
    url: 'catalog--kholodilniki/71639/list',
    selector: VendorPromoProductGroup.root,
    ignore: [
        {every: VendorPromoProductGroup.price},
    ],
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        initLazyWidgets(actions, 4000);
    },
    capture() {},
};
