import {makeSuite, makeCase, mergeSuites} from 'ginny';

// page-objects
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.touch';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';

/**
 * Тест на работу кнопки "В корзину"
 */
export default makeSuite('Кнопка "В корзину", поведение каунтера', {
    environment: 'kadavr',
    feature: 'Кнопка "В корзину".',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                notification: () => this.createPageObject(Notification),
                header: () => this.createPageObject(Header),
                cartEntryPoint: () => this.createPageObject(CartEntryPoint, {
                    parent: this.header,
                }),
            });
        },
        'По клику': {
            'каунтер увеличивается на единицу': makeCase({
                issue: 'MARKETFRONT-13236',
                id: 'm-touch-3352',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);
                    await this.cartButton.click();

                    await this.browser.waitForVisible(CartEntryPoint.counter, 10000);

                    const counterText = await this.cartEntryPoint.getCounterText();

                    return this.expect(counterText)
                        .to.be.equal('1', 'Каунтер равен единице');
                },
            }),
        },
        'После перезагрузки': {
            'значение каунтера сохраняется': makeCase({
                id: 'm-touch-3353',
                issue: 'MARKETFRONT-13236',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);
                    await this.cartButton.click();
                    await this.browser.waitForVisible(CartPopup.root, 10000);
                    await this.browser.refresh();
                    await this.browser.waitForVisible(CartEntryPoint.counter, 10000);

                    const counterText = await this.cartEntryPoint.getCounterText();

                    return this.expect(counterText)
                        .to.be.equal('1', 'Каунтер равен единице');
                },
            }),
        },
    }),
});
