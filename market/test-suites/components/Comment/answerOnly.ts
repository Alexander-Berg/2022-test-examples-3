'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на комментарий с ответом
 *
 * @param {PageObject.Comment} comment
 * @param {Object} params
 * @param {boolean} [params.editable] – флаг возможности редактирования
 */
export default makeSuite('Ответ на комментарий.', {
    story: {
        'Если на комментарий вендора дан ответ,': {
            'кнопки «Изменить» и «Удалить» у этого комментария': {
                'не отображаются': makeCase({
                    async test() {
                        const {editable} = this.params;

                        if (editable === false) {
                            await this.comment.answerButton
                                .vndIsExisting()
                                .should.eventually.be.equal(false, 'Кнопка "Ответить" отсутствует');
                        }

                        if (editable === true) {
                            await this.comment.answerButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка "Ответить" отображается');
                        }

                        await this.comment.getText().should.eventually.be.equal('Мой комментарий.', 'Текст корректный');

                        await this.comment.editButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка "Изменить" отсутствует');

                        await this.comment.deleteButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Кнопка "Удалить" отсутствует');
                    },
                }),
            },
        },
    },
});
