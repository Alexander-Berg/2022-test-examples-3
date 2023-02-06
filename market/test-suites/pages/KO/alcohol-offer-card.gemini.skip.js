import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import {hideRegionPopup, hideParanja, hideMooa, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import GallerySlider from '@self/platform/spec/page-objects/components/Gallery/GallerySlider';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import SnippetPrice from '@self/project/src/components/SnippetPrice/__pageObject/SnippetPrice';
import {offers} from '@self/project/src/spec/gemini/configs/offers';

// https://yql.yandex-team.ru/Operations/XnjlBQtcPy66D6KwpgCoyU5JrdH1uGZd4jHKNRbzJdY= для алко-офферов
const OFFER_ID = offers.offerAlco.wareid;

export default {
    suiteName: 'AlcoholKO',
    url: `/offer/${OFFER_ID}`,
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'adult',
                value: '1:1:ADULT',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: SnippetPrice.root,
            capture(actions) {
                // Ждём появления картинки - чтобы получить падение если оффер протухнет
                actions.waitForElementToShow(GallerySlider.root, 3000);
            },
        },
    ],
};
