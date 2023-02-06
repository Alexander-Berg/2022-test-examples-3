import {makeSuite, makeCase, mergeSuites} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// page-objects
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';

/**
 * Тест на работу кнопки "В корзину"
 */
export default makeSuite('Кнопка "В корзину".', {
    environment: 'kadavr',
    feature: 'Кнопка "В корзину".',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                cartPopup: () => this.createPageObject(CartPopup),
            });
        },
        'По умолчанию': {
            'присутствует на странице': makeCase({
                id: 'm-touch-3350',
                issue: 'MARKETFRONT-13236',
                async test() {
                    const isCartButtonExisting = await this.cartButton.isVisible();

                    return this.expect(isCartButtonExisting)
                        .to.be.equal(true, 'Кнопка "В корзину" присутствует на странице');
                },
            }),
        },
        'По клику': {
            'появляется попап': makeCase({
                id: 'm-touch-3351',
                issue: 'MARKETFRONT-13236',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const cartButtonSelector = await this.cartButton.getSelector();
                    await this.browser.scroll(cartButtonSelector);
                    await this.cartButton.click();

                    await this.browser.waitForVisible(CartPopup.root, 10000);

                    await this.cartPopup.waitForText('Товар в корзине');
                    const linkHref = await this.cartPopup.getLinkHref();

                    let expectedUrl = await this.browser.yaBuildFullUrl(PAGE_IDS_COMMON.CART);
                    if (expectedUrl.indexOf('//') === 0) {
                        expectedUrl = `https:${expectedUrl}`;
                    }

                    return this.browser.allure.runStep(
                        'Проверяем, что ссылка в нотификации ведёт на страницу Корзины',
                        () => this.expect(linkHref, 'Ссылка верная')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                            })
                    );
                },
            }),
        },
    }),
});
