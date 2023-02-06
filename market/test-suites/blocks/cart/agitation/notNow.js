import {makeSuite, makeCase} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

export default makeSuite('По нажатию на кнопку "Не сейчас" в попапе "войдите, чтобы получить скидки"', {
    id: 'marketfront-5821',
    story: {
        async beforeEach() {
            this.setPageObjects({
                notNowButton: () => this.createPageObject(Clickable, {
                    parent: this.modal,
                    root: '[data-autotest-id="notNow"]',
                }),
            });

            await this.cartCheckoutButton.waitForButtonEnabled();
            await this.cartCheckoutButton.goToCheckout();
            await this.modal.waitForVisible();
        },
        'Переходим на страницу оформления': makeCase({
            async test() {
                await this.browser.yaWaitForChangeUrl(() => this.notNowButton.click());
                const [openedUrl, expectedPath] = await Promise.all([
                    this.browser.getUrl(),
                    this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT2),
                ]);

                await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                    skipProtocol: true,
                    skipHostname: true,
                });
            },
        }),
    },
});
