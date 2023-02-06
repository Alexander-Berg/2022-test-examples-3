'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отмену раскрытия формы ответа
 * @param {PageObject.AnswerForm} root - форма ответа
 * @param {PageObject.AnswerFormToggle} toggle - тогглер формы ответа
 * @param {Object} params
 * @param {boolean} [params.exists] – флаг присутствия кнопки ответить
 * @param {string} [params.answerButtonText] - альтернативное название кнопки "Ответить"
 */
export default makeSuite('Отмена ответа на отзыв.', {
    story: {
        'При закрытии': {
            'форма скрывается': makeCase({
                async test() {
                    const {answerButtonText = 'Ответить'} = this.params;

                    if (this.params.exists === false) {
                        return this.toggle
                            .isVisible()
                            .should.eventually.be.equal(false, `Кнопка "${answerButtonText}" отсутствует`);
                    }

                    await this.toggle
                        .isVisible()
                        .should.eventually.equal(true, `Кнопка "${answerButtonText}" отображается`);

                    await this.toggle.click();

                    await this.toggle
                        .getText()
                        .should.eventually.be.equal('Закрыть', 'Текст кнопки изменился на "Закрыть"');

                    await this.root.isVisible().should.eventually.equal(true, 'Форма ответа отображается');

                    await this.root.userSnippet
                        .isVisible()
                        .should.eventually.equal(true, 'Сниппет пользователя отображается');

                    await this.toggle.click();

                    await this.toggle
                        .getText()
                        .should.eventually.be.equal(
                            answerButtonText,
                            `Текст кнопки изменился на "${answerButtonText}"`,
                        );

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
                },
            }),
        },
    },
});
