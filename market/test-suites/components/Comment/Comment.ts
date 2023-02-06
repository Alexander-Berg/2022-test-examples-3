'use strict';

import {makeSuite, makeCase} from 'ginny';
import moment from 'moment';

moment.locale('ru');

const currentDate = moment().format('LL');

/**
 * Тест на комментарий
 * @param {PageObject.Comment} comment
 * @param {Object} params
 * @param {boolean} [params.editable] – флаг возможности редактирования
 */
export default makeSuite('Комментарий.', {
    story: {
        'При редактировании': {
            'текст изменяется': makeCase({
                async test() {
                    if (this.params.editable === false) {
                        return this.comment.editButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка "Изменить" отсутствует');
                    }

                    await this.comment.editButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Изменить" отображается');

                    const text = 'Съещь ещё этих сладких булочек';

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

                    await this.comment.setText(text);

                    await this.comment.submit();

                    // Ожидаем появления компонента с текстом комментария.
                    await this.browser.waitUntil(
                        () => this.comment.textField.isVisible(),
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        'Форма перешла в режим чтения',
                    );

                    await this.comment
                        .getText()
                        .should.eventually.contain(text, `Текст комментария содержит "${text}"`);

                    await this.comment
                        .getCreationDate()
                        .should.eventually.be.equal(currentDate, `Дата создания обновилась на "${currentDate}"`);

                    await this.comment.editButton
                        .getText()
                        .should.eventually.be.equal('Изменить', 'Название кнопки изменилось на "Изменить"');
                },
            }),
        },
    },
});
