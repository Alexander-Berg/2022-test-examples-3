import PanelAside from '@self/platform/components/ProductOffers/PanelAside/__pageObject';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KMOffersFilters',
    before(actions) {
        hideProductTabs(actions);
    },
    selector: PanelAside.root,
    capture() {},
};
