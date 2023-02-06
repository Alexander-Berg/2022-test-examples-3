import {makeSuite, makeCase, mergeSuites} from 'ginny';

// page-objects
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.desktop';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
/**
 * Тест на работу кнопки "В корзину" и каунтер
 */
export default makeSuite('Кнопка "В корзину", поведение общего каунтера Корзины', {
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton),
                cartEntryPoint: () => this.createPageObject(CartEntryPoint),
            });
        },
        'По клику': {
            'каунтер увеличивается на единицу': makeCase({
                id: 'm-touch-3352',
                issue: 'MARKETFRONT-13258',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.cartButton.click();

                    await this.browser.waitForVisible(CartEntryPoint.root, 10000);

                    const counterText = await this.cartEntryPoint.getCounterText();

                    return this.expect(counterText)
                        .to.be.equal('1', 'Каунтер равен единице');
                },
            }),
        },
        'После перезагрузки': {
            'значение каунтера сохраняется': makeCase({
                id: 'm-touch-3353',
                issue: 'MARKETFRONT-13258',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.cartButton.click();
                    await this.browser.waitForVisible(CartPopup.root, 10000);
                    await this.browser.refresh();
                    await this.browser.waitForVisible(CartEntryPoint.root, 10000);

                    const counterText = await this.cartEntryPoint.getCounterText();

                    return this.expect(counterText)
                        .to.be.equal('1', 'Каунтер равен единице');
                },
            }),
        },
    }),
});
