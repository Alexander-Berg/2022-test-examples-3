'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отсутствие кнопок изменения/удаления ответа
 * @param {PageObject.Question} question
 */
export default makeSuite('Невозможность удаления ответа, на который уже есть комментарии.', {
    id: 'vendor_auto-427',
    issue: 'VNDFRONT-1812',
    story: {
        'При наличии комментария на ответ': {
            'кнопки изменения и удаления отсутствуют': makeCase({
                async test() {
                    await this.question.firstAnswerEditButton
                        .vndIsExisting()
                        .should.eventually.be.equal(false, 'Кнопка "Изменить" отсутствует');

                    await this.question.firstAnswerDeleteButton
                        .vndIsExisting()
                        .should.eventually.be.equal(false, 'Кнопка "Удалить" отсутствует');
                },
            }),
        },
    },
});
