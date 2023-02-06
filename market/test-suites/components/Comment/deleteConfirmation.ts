'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на подтверждение удаления комментария
 *
 * @param {PageObject.Comment} comment
 * @param {Object} params
 * @param {boolean} [params.editable] – флаг возможности редактирования
 * @param {string} [params.messageText] – всплывающее сообщение
 */
export default makeSuite('Удаление комментария.', {
    environment: 'kadavr',
    story: {
        'При удалении комментария': {
            'отмена и подтверждение удаления': {
                'работают корректно': makeCase({
                    async test() {
                        this.setPageObjects({
                            message() {
                                return this.createPageObject('Messages');
                            },
                        });

                        const {messageText, editable} = this.params;

                        if (editable === false) {
                            return this.comment.deleteButton
                                .vndIsExisting()
                                .should.eventually.be.equal(false, 'Кнопка "Удалить" отсутствует');
                        }

                        await this.comment.deleteButton
                            .isVisible()
                            .should.eventually.be.equal(true, 'Кнопка "Удалить" отображается');

                        await this.browser.allure.runStep('Нажимаем на кнопку "Удалить"', () =>
                            this.comment.deleteButton.click(),
                        );

                        await this.browser.allure.runStep('Отменяем удаление комментария', () =>
                            this.browser.alertDismiss(),
                        );

                        await this.browser.allure.runStep('Ещё раз нажимаем на кнопку "Удалить"', () =>
                            this.comment.deleteButton.click(),
                        );

                        await this.browser.allure.runStep('Подтверждаем удаление комментария', () =>
                            this.browser.alertAccept(),
                        );

                        await this.allure.runStep('Ожидаем появления сообщения об успешном удалении комментария', () =>
                            this.message.waitForExist(),
                        );

                        if (messageText) {
                            await this.message
                                .getMessageText()
                                .should.eventually.be.equal(messageText, 'В сообщении отображается корректный текст');
                        }

                        await this.comment
                            .isExisting()
                            .should.eventually.be.equal(false, 'Удалённый комментарий не отображается');
                    },
                }),
            },
        },
    },
});
