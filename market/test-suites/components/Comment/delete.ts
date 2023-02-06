'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на удаление комментария
 *
 * @param {PageObject.Comment} comment
 * @param {Object} params
 * @param {boolean} [params.editable] – флаг возможности редактирования
 * @param {string} [params.messageText] – всплывающее сообщение
 * @param {string} [params.confirmText] – текст подтверждения удаления
 */
export default makeSuite('Удаление комментария.', {
    story: {
        'При клике на "Удалить"': {
            'комментарий скрывается': makeCase({
                async test() {
                    this.setPageObjects({
                        message() {
                            return this.createPageObject('Messages');
                        },
                    });

                    const {confirmText, messageText, editable} = this.params;

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

                    if (confirmText) {
                        await this.browser.allure.runStep('Получаем текст подтвержения', () =>
                            this.browser
                                .alertText()
                                .should.eventually.be.equal(confirmText, 'Текст подтверждения корректный'),
                        );

                        await this.browser.allure.runStep('Подтверждаем удаление комментария', () =>
                            this.browser.alertAccept(),
                        );
                    }

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
});
