import {makeSuite, makeCase} from 'ginny';
import {
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

// page-objects
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';

// constants
import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';

// fixtures
import {
    productWithCPADefaultOfferAndUnitInfo,
    phoneProductRoute,
} from '@self/platform/spec/hermione/fixtures/product';


export default makeSuite('Попап. Единицы измерения.', {
    id: 'm-touch-3956',
    issue: 'MARKETFRONT-78107',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                popupCartCounter: () => this.createPageObject(CounterCartButton, {
                    parent: CartPopup.root,
                }),

                cartButton: () => this.createPageObject(CartButton, {
                    parent: DefaultOffer.root,
                }),
            });

            const dataMixin = {
                data: {
                    search: {
                        total: 1,
                        totalOffers: 1,
                    },
                },
            };

            await this.browser.setState('Carter.items', []);
            await this.browser.setState('report', mergeState([
                productWithCPADefaultOfferAndUnitInfo,
                dataMixin,
            ]));
            await this.browser.yaOpenPage(PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT, phoneProductRoute);
        },
        'Единицы измерения в счетчике в попапе присутсвуют': makeCase({
            async test() {
                await this.browser.yaWaitForPageReady();
                const cartButtonSelector = await this.cartButton.getSelector();
                await this.browser.scroll(cartButtonSelector);
                await this.cartButton.click();

                await this.browser.waitForVisible(CartPopup.root, 10000);

                const counterText = await this.popupCartCounter.getCounterText();

                return this.expect(counterText)
                    .to.be.equal('1 уп', 'Единицы измерения в счетчике в попапе присутствуют.');
            },
        }),
    },
});
