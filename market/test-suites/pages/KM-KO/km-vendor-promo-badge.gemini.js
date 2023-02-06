import VendorTagBadge from '@self/project/src/components/VendorTagBadge/__pageObject__';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'VendorPromoBadgeKM',
    url: '/product--yandex-stantsiia-umnaia-kolonka-dlia-umnogo-doma/1971204201',
    selector: VendorTagBadge.root,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideProductTabs(actions);
    },
    capture() {},
};

