import {prepareSuite, makeSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import PopupComplainButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/popup-complain/__button';
import ComplainButton from '@self/platform/spec/page-objects/components/ComplainButton';
import PopupComplainForm from '@self/platform/spec/page-objects/components/ComplainPopup';

export default makeSuite('Кнопка "Пожаловаться"', {
    environment: 'kadavr',
    story: prepareSuite(PopupComplainButtonSuite, {
        feature: 'Пожаловаться',
        meta: {
            id: 'marketfront-2303',
            issue: 'MARKETVERSTKA-27527',
        },
        pageObjects: {
            complainButton() {
                return this.createPageObject(ComplainButton);
            },
            complainPopupForm() {
                return this.createPageObject(PopupComplainForm);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('report', createOffer({
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        offercard: '/redir/offercard',
                        geo: '/redir/geo',
                    },
                    shop: {
                        slug: 'shop',
                        name: 'shop',
                        id: 1,
                    },
                }, 42));
                await this.browser.yaOpenPage('market:offer', {offerId: 42});
            },
        },
    }),
});
