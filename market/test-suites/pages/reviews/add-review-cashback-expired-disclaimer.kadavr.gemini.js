import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import utils from '@yandex-market/gemini-extended-actions/';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideRegionPopup} from '@self/platform/spec/gemini/helpers/hide';
import CashbackDisclaimerSuite from '@self/platform/spec/gemini/test-suites/blocks/reviewAdd/cashbackDisclaimer';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const testUser = profiles.ugctest3;

const productId = 14236972;
const slug = 'random-fake-slug';

const reportProduct = createProduct({
    type: 'model',
    categories: [{
        id: 123,
    }],
    slug,
    deletedId: null,
}, productId);

const url = `/product--${slug}/${productId}/reviews/add?plusPayment=1`;

export default {
    suiteName: 'add-review-cashback-expired-disclaimer[KADAVR]',
    url,
    before(actions) {
        createSession.call(actions);
        setState.call(actions, 'report', reportProduct);
        setDefaultGeminiCookies(actions);
        utils.authorize.call(actions, {
            login: testUser.login,
            password: testUser.password,
            url,
        });
        hideRegionPopup(actions);
    },
    after(actions) {
        deleteSession.call(actions);
    },
    childSuites: [
        CashbackDisclaimerSuite,
    ],
};
