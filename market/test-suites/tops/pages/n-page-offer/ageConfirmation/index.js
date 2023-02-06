import {makeSuite, prepareSuite} from 'ginny';
import {mergeState, createOffer, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import ConfirmationPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/offercard';
import AdultWarning from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject/';

const OFFER_ID = 'uQizLmsYjkLixn5SRhgitQ';
const PRODUCT_ID = 420;

export default makeSuite('Подтверждение возраста', {
    environment: 'kadavr',
    story: prepareSuite(ConfirmationPopupSuite, {
        params: {
            offerId: OFFER_ID,
        },
        pageObjects: {
            adultConfirmationPopup() {
                return this.createPageObject(AdultWarning);
            },
        },
        hooks: {
            async beforeEach() {
                const productId = PRODUCT_ID;
                const offerId = OFFER_ID;

                /**
                 * Реализация подтверждения возраста на КО отличается от остальных мест:
                 * проверка завязана на наличие "alco" в categories[0].kinds
                 */
                const offer = createOffer({
                    model: {
                        id: productId,
                    },
                    shop: {
                        entity: 'shop',
                        id: 2,
                        name: 'shop',
                        slug: 'shop',
                    },
                    urls: {
                        encrypted: '/redir/test',
                        decrypted: '/redir/test',
                        geo: '/redir/test',
                        offercard: '/redir/test',
                    },
                    categories: [
                        {
                            kinds: ['alco'],
                        },
                    ],
                    slug: 'test-offer',
                }, offerId);
                const product = createProduct({slug: 'product'}, productId);
                const state = mergeState([
                    offer,
                    product,
                    {
                        data: {
                            search: {
                                total: 1,
                                adult: true,
                            },
                        },
                    },
                ]);

                await this.browser.setState('report', state);
                await this.browser.yaOpenPage('market:offer', {offerId});
            },
        },
    }),
});
