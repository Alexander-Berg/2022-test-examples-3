'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отмену редактирования комментария
 * @param {PageObject.Comment} comment
 * @param {Object} params
 * @param {boolean} [params.editable] – флаг возможности редактирования
 */
export default makeSuite('Отмена редактирования комментария.', {
    story: {
        'При отмене': {
            'возвращает исходный текст': makeCase({
                async test() {
                    if (this.params.editable === false) {
                        return this.comment.editButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка "Изменить" отсутствует');
                    }

                    await this.comment.editButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Изменить" отображается');

                    const text = await this.comment.getText();
                    const creationDate = await this.comment.getCreationDate();

                    await this.browser.allure.runStep('Нажимаем на кнопку "Изменить"', () =>
                        this.comment.editButton.click(),
                    );

                    await this.comment.editButton
                        .getText()
                        .should.eventually.be.equal('Отменить', 'Название кнопки изменилось на "Отменить"');

                    // Проверяем, что появилась кнопка отправки формы.
                    await this.comment.submitButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Форма перешла в режим редактирования');

                    await this.comment.setText('Параллелограмм');

                    await this.browser.allure.runStep('Нажимаем на кнопку "Отменить"', () =>
                        this.comment.editButton.click(),
                    );

                    await this.comment.editButton
                        .getText()
                        .should.eventually.be.equal('Изменить', 'Название кнопки изменилось на "Изменить"');

                    // Ожидаем появления компонента с текстом комментария.
                    await this.browser.waitUntil(
                        () => this.comment.textField.isVisible(),
                        this.browser.options.waitforTimeout,
                        'Форма перешла в режим чтения',
                    );

                    await this.comment
                        .getText()
                        .should.eventually.be.equal(text, 'Текст комментария вернулся к исходному');

                    await this.comment
                        .getCreationDate()
                        .should.eventually.be.equal(creationDate, 'Дата создания не изменилась');
                },
            }),
        },
    },
});
