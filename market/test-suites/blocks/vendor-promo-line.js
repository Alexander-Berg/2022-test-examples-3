import VendorProductLinePromo from '@self/platform/spec/page-objects/VendorProductLinePromo';
import VertProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import {hideTooltip} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'VendorProductLinePromo',
    selector: VendorProductLinePromo.root,
    ignore: [
        {every: VendorProductLinePromo.price},
        {every: VertProductSnippet.root},
    ],
    before(actions) {
        hideTooltip(actions);
        actions.waitForElementToShow(`${VendorProductLinePromo.price}`, 10000);
    },
    capture() {},
};
