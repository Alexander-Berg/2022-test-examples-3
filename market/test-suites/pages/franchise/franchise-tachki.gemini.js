import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import GallerySlider from '@self/platform/spec/page-objects/components/Gallery/GallerySlider';
import VideoFrame from '@self/platform/spec/page-objects/widgets/parts/VideoFrame';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideModalFloat, hideMooa, hideParanja, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

export default {
    suiteName: 'FranchiseTachki',
    url: '/franchise--tachki-disney/14713996',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        initLazyWidgets(actions, 1500);
        disableAnimations(actions);
    },
    selector: [
        MainSuite.selector,
    ],
    ignore: [
        VideoFrame.root,
        {every: GallerySlider.root},
        {every: '[data-zone-name="navnode"]'},
        {every: ProductSnippet.root},
    ],
    capture() {},
};
