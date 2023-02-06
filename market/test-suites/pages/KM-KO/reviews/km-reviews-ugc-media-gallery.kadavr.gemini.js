import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {hideProductTabs, hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import UgcMediaGallerySuite from '@self/platform/spec/gemini/test-suites/blocks/UgcMediaGallery';

import {reportProduct, reviewWithPhotosSchema} from '../mocks/product-with-review.mock';

export default {
    suiteName: 'KM-reviews-UGC-media-gallery[KADAVR]',
    url: '/product--random-fake-slug/14236972/reviews',
    before(actions) {
        createSession.call(actions);
        setState.call(actions, 'report', reportProduct);
        setState.call(actions, 'schema', reviewWithPhotosSchema);
        setDefaultGeminiCookies(actions);
        hideProductTabs(actions);
        hideRegionPopup(actions);
        disableAnimations(actions);
        initLazyWidgets(actions, 3000);
    },
    after(actions) {
        deleteSession.call(actions);
    },
    childSuites: [
        UgcMediaGallerySuite,
    ],
};
