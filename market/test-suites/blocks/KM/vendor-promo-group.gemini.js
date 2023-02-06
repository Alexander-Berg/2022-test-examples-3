import VendorPromoProductGroup from '@self/platform/spec/page-objects/widgets/content/VendorPromoProductGroup';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


export default {
    suiteName: 'VendorPromoProductGroup',
    selector: VendorPromoProductGroup.root,
    ignore: [
        {every: VendorPromoProductGroup.price},
    ],
    before(actions) {
        initLazyWidgets(actions);
    },
    capture() {},
};

