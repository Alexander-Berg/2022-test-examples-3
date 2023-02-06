import utils from '@yandex-market/gemini-extended-actions/';
import ProductReviewFormMicro from '@self/platform/spec/gemini/test-suites/blocks/ProductReviewFormMicro';
import {
    hideRegionPopup,
    hideModalFloat,
    hideHeader,
    hideHeadBanner,
} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

export default {
    suiteName: 'model-card-review-form-micro-button',
    // тестинг
    // url: '/product--smartfon-samsung-galaxy-s10-8-128-gb/148921003',
    url: '/product--elektronnaia-kniga-digma-e635-4-gb/1952186490',
    before(actions) {
        utils.authorize.call(actions, {
            login: profiles.reviewsfortest.login,
            password: profiles.reviewsfortest.password,
            url: '/product--elektronnaia-kniga-digma-e635-4-gb/1952186490',
        });
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideHeader(actions);
        hideHeadBanner(actions);
        initLazyWidgets(actions, 5000);
    },
    after(actions) {
        utils.logout.call(actions);
    },
    childSuites: [
        ProductReviewFormMicro,
    ],
};
