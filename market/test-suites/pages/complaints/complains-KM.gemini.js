import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject/';
import ComplainButton from '@self/platform/spec/page-objects/components/ComplainButton';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import ProductSpec from '@self/platform/components/ProductFullSpecs/__pageObject';
import ComplainPopupForm from '@self/platform/widgets/parts/ComplainFormPopup/__pageObject';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {
    hideRegionPopup,
    hideHeadBanner,
    hideDevTools,
    hideHeader2,
    hideYndxBug,
    hideProductTabs,
} from '@self/platform/spec/gemini/helpers/hide';


export default {
    suiteName: 'KMComplain',
    url: 'product--smartfon-xiaomi-redmi-note-8-pro-6-128gb/572745038/',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideHeader2(actions);
        hideHeadBanner(actions);
        hideDevTools(actions);
        hideYndxBug(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        {
            suiteName: 'Popup',
            selector: ComplainPopupForm.root,
            before(actions, find) {
                // Тут нужен ховер мышью над ДО, иначе не появится иконка и gemini не сможет по ней кликнуть
                actions.mouseMove(DefaultOffer.root);
                actions.waitForElementToShow(ComplainButton.root, 500);
                actions.click(find(ComplainButton.root));
                actions.waitForElementToShow(ModalFloat.roundingNormal, 1000);
                // чтобы попап догрузился и не просвечивал
                actions.wait(500);
            },
            capture() {
            },
        },
        {
            suiteName: 'PopupSpec',
            url: 'product--smartfon-xiaomi-redmi-note-8-pro-6-128gb/572745038/spec/',
            selector: ModalFloat.roundingNormal,
            before(actions, find) {
                actions.click(find(`${ProductSpec.complainLink} span`));
                actions.wait(500);
            },
            capture() {
            },
        },
    ],
};
