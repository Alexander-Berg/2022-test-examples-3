import utils from '@yandex-market/gemini-extended-actions/';
import Footer from '@self/platform/spec/page-objects/footer-market';
import FooterHistory from '@self/platform/spec/page-objects/footer-history';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';
import {hideElementBySelector} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'Footer',
    selector: Footer.root,
    ignore: {every: Footer.stats},
    before(actions) {
        utils.authorize.call(actions, {
            login: profiles.Recomend2017.login,
            password: profiles.Recomend2017.password,
            url: '/my/wishlist',
        });
    },
    capture(actions) {
        hideElementBySelector(actions, FooterHistory.root);
        hideElementBySelector(actions, '[data-zone-name="footerSubscription"]');
    },
    after(actions) {
        utils.logout.call(actions);
    },
};
