import {makeSuite, makeCase} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// page-objects
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import AmountSelect from '@self/project/src/components/AmountSelect/__pageObject';

// mocks
import {product, productId, slug} from './fixtures/product';
import {offer} from './fixtures/offer';

export default makeSuite('Попап. Единицы измерения.', {
    environment: 'kadavr',
    id: 'marketfront-5402',
    issue: 'MARKETFRONT-78853',
    story: {
        async beforeEach() {
            this.setPageObjects({
                popupCartCounter: () => this.createPageObject(AmountSelect, {
                    parent: CartPopup.root,
                }),
                cartButton: () => this.createPageObject(CartButton),
            });

            await this.browser.setState('report', mergeState([
                product,
                offer,
            ]));

            return this.browser.yaOpenPage('market:product', {productId, slug});
        },
        'Единицы измерения в счетчике в попапе присутсвуют': makeCase({
            async test() {
                await this.browser.yaWaitForPageReady();
                await this.cartButton.click();

                await this.browser.waitForVisible(CartPopup.root, 10000);

                const unitText = await this.popupCartCounter.getUnitText();

                return this.expect(unitText)
                    .to.be.equal('уп', 'Единицы измерения в счетчике в попапе присутствуют.');
            },
        }),
    },
});
