import {makeSuite, makeCase, mergeSuites} from 'ginny';

// page-objects
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import ExpressAddressPopup from '@self/root/src/widgets/content/ExpressAddressPopup/components/View/__pageObject';
/**
 * Тест на работу кнопки "В корзину"
 */
export default makeSuite('Кнопка "В корзину"', {
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton),
                cartPopup: () => this.createPageObject(CartPopup),
            });
        },
        'По клику': {
            'появляется попап': makeCase({
                id: 'm-touch-3351',
                issue: 'MARKETFRONT-13258',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.cartButton.click();

                    await this.browser.waitForVisible(CartPopup.root, 10000);

                    const isVisible = await this.browser.waitForVisible(CartPopup.root, 10000);
                    await this.browser.waitForVisible(ExpressAddressPopup.root, 5000, true); // попап гиперлокальности НЕ показался

                    return this.expect(isVisible).to.be.equal(true, 'Попап отобразился');
                },
            }),
        },
    }),
});
