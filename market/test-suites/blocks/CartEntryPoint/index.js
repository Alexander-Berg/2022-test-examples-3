import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';


/**
 * Тесты на блок CartEntryPoint
 * @property {PageObject.CartEntryPoint} cartEntryPoint
 */
export default makeSuite('Кнопка входа в Корзину.', {
    story: {
        'По клику': {
            'открывает страницу Корзины на нужном домене': makeCase({
                async test() {
                    await this.cartEntryPoint.isVisible();
                    const changedUrl = await this.browser
                        .yaWaitForChangeUrl(() => this.cartEntryPoint.click(), 10000);

                    let expectedUrl = await this.browser
                        .yaBuildFullUrl(PAGE_IDS_COMMON.CART);

                    if (expectedUrl.indexOf('//') === 0) {
                        expectedUrl = `https:${expectedUrl}`;
                    }

                    return this.browser.allure.runStep(
                        'Проверяем, что перешли на страницу Корзины',
                        () => this.expect(changedUrl, 'Перешли на страницу Корзины')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                            })
                    );
                },
            }),
        },
    },
});
