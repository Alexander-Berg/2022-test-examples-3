import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {hideRegionPopup, hideDevTools, hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

import VersusScrollBox from '@self/platform/spec/page-objects/widgets/content/VersusScrollBox/index';
import ProductPrice from '@self/platform/spec/page-objects/ProductPrice';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'VersusEntrypointsKM',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideProductTabs(actions);
        disableAnimations(actions);
        initLazyWidgets(actions, 5000);
    },
    selector: VersusScrollBox.root,
    ignore: [
        {every: ProductPrice.root},
    ],
    childSuites: [
        {
            suiteName: 'NumberAndColorCharacteristics',
            url: '/product--naushniki-apple-airpods-2-besprovodnaia-zariadka-chekhla-mrxj2/224174226',
            capture() {
            },
        },
        {
            suiteName: 'BooleanCharacteristics',
            url: '/product--kapous-professional/1807272041',
            capture() {
            },
        },
    ],
};
