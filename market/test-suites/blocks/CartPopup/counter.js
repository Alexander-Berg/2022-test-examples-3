import {makeSuite, makeCase, mergeSuites} from 'ginny';

// page-objects
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';

export default makeSuite('Попап "В корзину", поведение каунтера', {
    environment: 'kadavr',
    story: mergeSuites({
        'Кнопка "в корзину"': {
            'По клику': {
                'каунтер увеличивается на единицу': makeCase({
                    async test() {
                        await this.browser.yaWaitForPageReady();
                        const cartButtonSelector = await this.cartButton.getSelector();
                        await this.browser.scroll(cartButtonSelector);
                        await this.cartButton.click();

                        await this.browser.waitForVisible(CartPopup.root, 10000);

                        const counterText = await this.popupCartCounter.getCounterText();

                        return this.expect(counterText)
                            .to.be.equal('1', 'Каунтер равен единице');
                    },
                }),
            },
        },
        'Кнопки "+", "-"': {
            'по клику': {
                'значение каунтера изменяется корректно': makeCase({
                    async test() {
                        await this.browser.yaWaitForPageReady();
                        const cartButtonSelector = await this.cartButton.getSelector();
                        await this.browser.scroll(cartButtonSelector);
                        await this.cartButton.click();

                        await this.browser.waitForVisible(CartPopup.root, 10000);

                        const popupCartCounterSelector = await this.popupCartCounter.getSelector();

                        await this.browser.waitForVisible(popupCartCounterSelector);

                        await this.popupCartCounter.increase.click();
                        await this.popupCartCounter.waitUntilCounterChanged(1, 2);

                        await this.popupCartCounter.decrease.click();
                        await this.popupCartCounter.waitUntilCounterChanged(2, 1);
                    },
                }),
            },
        },
    }),
});
