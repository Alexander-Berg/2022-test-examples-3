import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import ComplainSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/compain.gemini';

import CarouselSection from '@self/platform/spec/page-objects/Section';
import GallerySlider from '@self/platform/spec/page-objects/components/Gallery/GallerySlider';
import OfferComplainButton from '@self/platform/spec/page-objects/components/OfferComplaintButton';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Viewport from '@self/platform/spec/page-objects/Viewport';

import utils from '@yandex-market/gemini-extended-actions/';
import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {offers} from '@self/project/src/spec/gemini/configs/offers';


const OFFER_WAREID = offers.offerWithLinkedKM.wareid;

export default {
    suiteName: 'KO',
    url: `/offer/${OFFER_WAREID}`,
    before(actions) {
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: 'mrktcashback@yandex.ru',
            password: 'phrd11zc2',
            url: `/offer/${OFFER_WAREID}`,
        });
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, CarouselSection.root);
                // Ждём появления картинки - чтобы получить падение если оффер протухнет
                actions.waitForElementToShow(GallerySlider.root, 3000);
            },
            ignore: [
                {every: `${ScrollBox.root} ${Viewport.root} [data-zone-name="product"]`},
            ],
        },
        {
            suiteName: 'KOComplain',
            childSuites: [
                {
                    ...ComplainSuite,
                    before(actions, find) {
                        actions.click(find(OfferComplainButton.root));
                        // ждем, пока попап как следует прогрузится
                        actions.wait(1000);
                    },
                },
            ],
        },
    ],
};
