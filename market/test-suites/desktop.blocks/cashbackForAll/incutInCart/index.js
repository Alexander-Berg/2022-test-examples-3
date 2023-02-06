/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @start
 */

import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';

import incutInCart from '@self/root/src/spec/hermione/test-suites/blocks/cashbackForAll/incutInCart';

const suite = makeSuite('Виджет', {
    params: {
        shouldBeShown: 'Должна ли отображаться врезка',
    },
    story: {
        'Содержит корректный контент': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, отображение на странице',
                    () => this.cashbackForAllBanner.waitForVisible(!this.params.shouldBeShown)
                );

                if (this.params.shouldBeShown) {
                    await this.browser.allure.runStep(
                        'Проверяем текст заголовка',
                        () => this.cashbackForAllBanner.getTitle()
                            .should.eventually.to.be.equal(
                                'Получите от \n 200\n баллов\n за покупку,',
                                'Заголовок должен содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст информационного сообщения',
                        () => this.cashbackForAllBanner.getText()
                            .should.eventually.to.be.equal(
                                'если подключите Плюс сейчас',
                                'Информационное сообщение должно содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст ссылки',
                        () => this.cashbackForAllBanner.getPromoLinkText()
                            .should.eventually.to.be.equal(
                                'Подробнее',
                                'Ссылка должна содержать корректный текст'
                            )
                    );

                    // TODO Добавить логику проверки открытия попапа подключения Плюса, когда он появится
                }
            },
        }),
    },
});

export default makeSuite('"Кешбэк для всех".', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(incutInCart(suite))
    ),
});
/**
 * @expFlag all_plus-4-all
 * @ticket MARKETPROJECT-8396
 * @end
 */
