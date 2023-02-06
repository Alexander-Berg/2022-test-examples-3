import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Закрытие попапа "Войдите, чтобы получать скидки".', {
    id: 'marketfront-5817',
    story: {
        async beforeEach() {
            await this.cartCheckoutButton.waitForButtonEnabled();
            await this.cartCheckoutButton.goToCheckout();
            await this.modal.waitForVisible();
        },
        'Кликом по крестику': makeCase({
            async test() {
                await this.modal.clickOnCrossButton();
                await this.modal.waitForInvisible();
            },
        }),
        'Кликом снаружи попапа': makeCase({
            async test() {
                const content = await this.modal.content;
                const {width} = await this.browser.getElementSize(content.selector);
                await this.modal.clickOutsideContent((width / -2) - 1, 0);
                await this.modal.waitForInvisible();
            },
        }),
        'Нажатием на клавишу ESC': makeCase({
            async test() {
                await this.modal.closeOnEscape();
                await this.modal.waitForInvisible();
            },
        }),
    },
});
