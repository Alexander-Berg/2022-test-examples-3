import {makeSuite, makeCase} from 'ginny';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Переход на страницу паспорта', {
    id: 'marketfront-5818',
    story: {
        async beforeEach() {
            this.setPageObjects({
                loginButton: () => this.createPageObject(Button, {
                    parent: this.modal,
                    root: `[data-auto="checkout-auth-continue"] ${Button.root}`,
                }),
            });

            await this.cartCheckoutButton.waitForButtonEnabled();
            await this.cartCheckoutButton.goToCheckout();
            await this.modal.waitForVisible();
            await this.browser.yaWaitForChangeUrl(() => this.loginButton.click());
        },
        'При нажатии паспортной кнопки "назад" переходим в корзину': makeCase({
            async test() {
                const backButton = await this.browser.element('[data-t="backpane"]');
                await this.browser.yaWaitForChangeUrl(() => this.browser.click(backButton.selector));
                const [openedUrl, expectedPath] = await Promise.all([
                    this.browser.getUrl(),
                    this.browser.yaBuildURL(PAGE_IDS_COMMON.CART),
                ]);
                await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                    skipProtocol: true,
                    skipHostname: true,
                });
            },
        }),
        'При нажатии нативной кнопки "назад" переходим в корзину': makeCase({
            async test() {
                await this.browser.yaWaitForChangeUrl(() => this.browser.back());
                const [openedUrl, expectedPath] = await Promise.all([
                    this.browser.getUrl(),
                    this.browser.yaBuildURL(PAGE_IDS_COMMON.CART),
                ]);
                await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                    skipProtocol: true,
                    skipHostname: true,
                });
            },
        }),
    },
});
