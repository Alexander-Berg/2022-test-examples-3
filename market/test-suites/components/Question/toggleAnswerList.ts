'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на раскрытие списка ответов после сохранения
 *
 * @param {PageObject.Question} question
 * @param {PageObject.AnswerForm} answerForm - форма ответа
 * @param {PageObject.AnswerFormToggle} toggle - тогглер формы ответа
 * @param {PageObject.Messages} messages - всплывающее сообщение
 * @param {Object} params
 * @param {boolean} params.text – текст раскрывашки
 * @param {boolean} params.messageText – текст всплывающего сообщения
 * @param {boolean} [params.exists] – флаг присутствия кнопки ответи
 * @param {boolean} [params.answerButtonText] – текст кнопки ответа
 * @param {boolean} [params.expectedTemplateText] – текст шаблона ответа
 */
export default makeSuite('Раскрытие списка ответов после сохранения.', {
    environment: 'kadavr',
    story: {
        'После сохранения ответа на вопрос': {
            'список ответов раскрывается': makeCase({
                async test() {
                    const {
                        text,
                        exists,
                        messageText,
                        answerButtonText = 'Ответить',
                        expectedTemplateText,
                    } = this.params;

                    if (exists === false) {
                        return this.toggle
                            .isVisible()
                            .should.eventually.be.equal(false, `Кнопка "${answerButtonText}" отсутствует`);
                    }

                    await this.question
                        .getAnswerListToggleText()
                        .should.eventually.be.equal(text, 'Текст раскрывашки корректный');

                    await this.toggle.click();

                    await this.toggle
                        .getText()
                        .should.eventually.be.equal('Закрыть', 'Текст кнопки изменился на "Закрыть"');

                    await this.answerForm.isVisible().should.eventually.be.equal(true, 'Форма ответа отображается');

                    await this.answerForm.button
                        .isEnabled()
                        .should.eventually.be.equal(true, 'Кнопка отправки разблокирована');

                    if (expectedTemplateText) {
                        await this.answerForm.input
                            .getValue()
                            .should.eventually.be.equal(expectedTemplateText, 'Текст шаблона в форме корректный');
                    }

                    await this.answerForm.setText('test');

                    await this.answerForm.submit();

                    await this.allure.runStep('Ожидаем появления сообщения о сохранении ответа', () =>
                        this.messages.waitForExist(),
                    );

                    await this.messages
                        .getMessageText()
                        .should.eventually.be.equal(messageText, 'Текст сообщения корректный');

                    await this.question
                        .getAnswerListToggleText()
                        .should.eventually.be.equal('Скрыть ответы', 'Текст раскрывашки изменился на "Скрыть ответы"');

                    await this.question.answerList
                        .isVisible()
                        .should.eventually.be.equal(true, 'Список ответов раскрылся');
                },
            }),
        },
    },
});
