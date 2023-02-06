import {
    hideRegionPopup,
    hideVersusHeader,
    hideHeadBanner,
    hideHeader2,
    hideTopmenu,
    hideFooterSubscriptionWrap,
    hideFooter,
    hideAllVersusQuestions,
    hideAllVersusReview,
    hideVersusCommentaries,
} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import VersusHeader from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/Header';
import ProductCard from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/ProductCard';
import VersusReview from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/Reviews';
import VersusQuestion from '@self/platform/spec/page-objects/widgets/content/VersusContent/components/Questions';
import VersusCommentaries from '@self/platform/spec/page-objects/widgets/content/Commentaries/view';
import VersusScrollBox from '@self/platform/spec/page-objects/widgets/content/VersusScrollBox/index';
import ProductPrice from '@self/platform/spec/page-objects/ProductPrice';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const VERSUS_DEFAULT_ID = 1235;
const VERSUS_QUESTIONS_ID = 12354;

export default {
    suiteName: 'VersusPage',
    url: {
        pathname: `/versus--any/${VERSUS_DEFAULT_ID}`,
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideHeadBanner(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
        hideHeader2(actions);
    },
    childSuites: [
        {
            suiteName: MainSuite.suiteName,
            selector: MainSuite.selector,
            ignore: [
                VersusScrollBox.root,
                {every: `${ProductCard.root} [data-zone-name="offers"]`},
            ],
            before(actions) {
                hideVersusHeader(actions);
                hideAllVersusReview(actions);
                hideAllVersusQuestions(actions);
                hideVersusCommentaries(actions);
                initLazyWidgets(actions, 5000);
                actions.waitForElementToShow(VersusScrollBox.root, 2000);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusHeader',
            selector: VersusHeader.root,
            before(actions) {
                disableAnimations(actions);

                // Скроллим вниз для появления заголовка
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function('window.scrollTo(0, 1200);'));
                actions.waitForElementToShow(VersusHeader.root, 2000);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusProductOffers',
            selector: `${ProductCard.root} [data-zone-name="offers"]`,
            before(actions) {
                hideVersusHeader(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusScrollBox',
            selector: VersusScrollBox.root,
            ignore: [
                {every: ProductPrice.root},
            ],
            before(actions) {
                disableAnimations(actions);
                initLazyWidgets(actions, 5000);
                actions.waitForElementToShow(VersusScrollBox.root, 2000);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusSomeReview',
            selector: `${VersusReview.root}`,
            before(actions) {
                hideVersusHeader(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusQuestions',
            url: {
                pathname: `/versus--any/${VERSUS_QUESTIONS_ID}`,
            },
            selector: `${VersusQuestion.root}`,
            before(actions) {
                hideVersusHeader(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'VersusCommentaries',
            selector: `${VersusCommentaries.root}`,
            before(actions) {
                hideVersusHeader(actions);
            },
            capture() {
            },
        },
    ],
};
