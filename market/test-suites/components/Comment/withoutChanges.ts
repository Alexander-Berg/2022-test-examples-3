'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на редактирование комментария без изменений
 * @param {PageObject.Comment} comment
 * @param {PageObject.Popup} popup
 * @param {Object} params
 * @param {boolean} [params.editable] – флаг возможности редактирования
 */
export default makeSuite('Редактирование комментария без изменений.', {
    story: {
        'При отправке': {
            'появляется подсказка': makeCase({
                async test() {
                    if (this.params.editable === false) {
                        return this.comment.editButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка "Изменить" отсутствует');
                    }

                    await this.comment.editButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Изменить" отображается');

                    await this.browser.allure.runStep('Нажимаем на кнопку "Изменить"', () =>
                        this.comment.editButton.click(),
                    );

                    // Проверяем, что появилась кнопка отправки формы.
                    await this.comment.submitButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Форма перешла в режим редактирования');

                    await this.comment.submit();

                    await this.browser.waitUntil(
                        () => this.popup.activeBodyPopup.isVisible(),
                        this.browser.options.waitforTimeout,
                        'Тултип появился',
                    );

                    await this.popup
                        .getActiveText()
                        .should.eventually.be.equal('Не было сделано изменений', 'Текст тултипа корректный');
                },
            }),
        },
    },
});
