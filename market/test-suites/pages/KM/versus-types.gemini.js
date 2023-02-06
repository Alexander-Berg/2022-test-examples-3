import {setCookies} from '@yandex-market/gemini-extended-actions';
import VersusScrollBox from '@self/platform/spec/page-objects/widgets/content/VersusScrollBox';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {
    hideRegionPopup,
} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

export default {
    suiteName: 'VersusTypes',
    before(actions) {
        setCookies.setRegionCookies.call(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            suiteName: 'VersusTypeProgressBar',
            url: '/product--gornyi-mtb-velosiped-stels-navigator-620-md-26-v010-2018/1964619971',
            selector: VersusScrollBox.root,
            before(actions) {
                setCookies.setRegionCookies.call(actions);
                hideRegionPopup(actions);
                initLazyWidgets(actions, 4000);
                disableAnimations(actions);
                actions.waitForElementToShow(VersusScrollBox.root, 5000);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusTypeColors',
            url: '/product--xiaomi-silikonovyi-remeshok-dlia-mi-band-3/165767871',
            selector: VersusScrollBox.root,
            before(actions) {
                setCookies.setRegionCookies.call(actions);
                hideRegionPopup(actions);
                initLazyWidgets(actions, 4000);
                disableAnimations(actions);
                actions.waitForElementToShow(VersusScrollBox.root, 5000);
            },
            capture() {
            },
        },
    ],
};
