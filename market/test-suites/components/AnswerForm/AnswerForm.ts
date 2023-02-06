'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на форму ответа
 * @param {PageObject.AnswerForm} root - форма ответа
 * @param {PageObject.AnswerFormToggle} toggle - тогглер формы ответа
 * @param {PageObject.Comment} answer - добавленный ответ (первый комментарий в списке)
 * @param {Object} params
 * @param {boolean} [params.exists] – флаг присутствия кнопки ответить
 * @param {boolean} [params.answerButtonVisible] - флаг присутствия кнопки "Ответить"
 * @param {string} [params.answerButtonText] - альтернативное название кнопки "Ответить"
 */
export default makeSuite('Ответ на отзыв.', {
    story: {
        'При отправке ответа': {
            'добавляется коммментарий': makeCase({
                async test() {
                    if (this.params.exists === false) {
                        return this.toggle
                            .isVisible()
                            .should.eventually.be.equal(false, 'Кнопка "Ответить" отсутствует');
                    }

                    await this.toggle.isVisible().should.eventually.equal(true, 'Кнопка "Ответить" отображается');

                    await this.toggle.click();

                    await this.toggle
                        .getText()
                        .should.eventually.be.equal('Закрыть', 'Текст кнопки изменился на "Закрыть"');

                    await this.root.isVisible().should.eventually.equal(true, 'Форма ответа отображается');

                    await this.root.userSnippet
                        .isVisible()
                        .should.eventually.equal(true, 'Сниппет пользователя отображается');

                    const text = 'Безмозглый широковещательный цифровой передатчик сужающихся экспонент.';

                    await this.root.setText(text);

                    await this.root.button.isEnabled().should.eventually.equal(true, 'Кнопка отправки разблокирована');

                    await this.root.submit();

                    await this.allure.runStep('Ожидаем скрытия формы с ответом', () =>
                        this.browser.waitUntil(
                            async () => {
                                const visible = await this.root.isVisible();

                                return visible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Форма ответа не скрылась',
                        ),
                    );

                    const {answerButtonVisible = true, answerButtonText = 'Ответить'} = this.params;

                    await this.browser.allure.runStep('Ожидаем появления комментария', () =>
                        this.answer.waitForVisible(),
                    );

                    await this.answer.getText().should.eventually.contain(text, `Текст комментария содержит "${text}"`);

                    if (answerButtonVisible === true) {
                        await this.answer.answerButton
                            .isVisible()
                            .should.eventually.be.equal(true, `Кнопка "${answerButtonText}" отображается`);
                    } else {
                        return this.answer.answerButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, `Кнопка "${answerButtonText}" отсутствует`);
                    }

                    await this.answer.editButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Изменить" отображается');

                    await this.answer.deleteButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Удалить" отображается');
                },
            }),
        },
    },
});
