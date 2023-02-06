'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на автоподстановку ответа на отзыв
 *
 * @param {PageObject.AnswerForm} answerForm - форма ответа
 * @param {PageObject.AnswerFormToggle} toggle - тогглер формы ответа
 * @param {Object} params
 * @param {boolean} [params.exists] – флаг присутствия кнопки ответить
 * @param {string} [params.answerButtonText] - альтернативное название кнопки "Ответить"
 * @param {string} params.expectedTemplateText - текст шаблона ответа
 */
export default makeSuite('Автоподстановка ответа', {
    environment: 'kadavr',
    story: {
        'При нажатии на кнопку «Ответить»': {
            'шаблон ответа в форме ответа': {
                отображается: makeCase({
                    async test() {
                        const {expectedTemplateText, answerButtonText = 'Ответить'} = this.params;

                        if (this.params.exists === false) {
                            return this.toggle
                                .isVisible()
                                .should.eventually.be.equal(false, `Кнопка "${answerButtonText}" отсутствует`);
                        }

                        await this.toggle
                            .isVisible()
                            .should.eventually.be.equal(true, `Кнопка "${answerButtonText}" отображается`);

                        await this.toggle.click();

                        await this.toggle
                            .getText()
                            .should.eventually.be.equal('Закрыть', 'Текст кнопки изменился на "Закрыть"');

                        await this.answerForm.isVisible().should.eventually.be.equal(true, 'Форма ответа отображается');

                        await this.answerForm.userSnippet
                            .isVisible()
                            .should.eventually.be.equal(true, 'Сниппет пользователя отображается');

                        await this.answerForm.button
                            .isEnabled()
                            .should.eventually.be.equal(true, 'Кнопка отправки разблокирована');

                        await this.answerForm.input
                            .getValue()
                            .should.eventually.be.equal(expectedTemplateText, 'Текст шаблона в форме корректный');
                    },
                }),
            },
        },
    },
});
