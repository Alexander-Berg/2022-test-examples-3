import ShopPage from '@self/platform/spec/page-objects/widgets/pages/ShopPage/view';
import ShopSummaryFactors from '@self/platform/spec/page-objects/widgets/content/ShopSummaryFactors/ShopSummaryFactors';
import {
    hideModalFloat,
    hideRegionPopup,
    hideMooa,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'ShopHub',
    url: '/shop--sviaznoi/3828',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideModalFloat(actions);
        hideRegionPopup(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            suiteName: 'Main',
            selector: ShopPage.root,
            ignore: [
                ShopSummaryFactors.root,
            ],
            capture() {
            },
        },
        {
            suiteName: 'ShopSummaryFactors',
            selector: ShopSummaryFactors.root,
            capture(actions) {
                // Ожидаем завершения анимации факторов магазина (зелёные кружки - прогресс бары)
                actions.wait(1000);
            },
        },
    ],
};
