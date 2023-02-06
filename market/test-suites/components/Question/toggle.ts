'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на раскрывашку
 * @param {PageObject.Question} question
 * @param {Object} params
 * @param {boolean} params.text – текст раскрывашки
 */
export default makeSuite('Развернуть/скрыть список ответов.', {
    id: 'vendor_auto-452',
    issue: 'VNDFRONT-1754',
    story: {
        'При клике на раскрывашку': {
            'показывает/скрывает список ответов': makeCase({
                async test() {
                    const {text} = this.params;

                    await this.question
                        .getAnswerListToggleText()
                        .should.eventually.be.equal(text, 'Текст раскрывашки корректный');

                    await this.question.toggleAnswers();

                    await this.question
                        .getAnswerListToggleText()
                        .should.eventually.be.equal('Скрыть ответы', 'Текст раскрывашки изменился на "Скрыть ответы"');

                    await this.question.answerList
                        .isVisible()
                        .should.eventually.be.equal(true, 'Список ответов раскрылся');

                    await this.question.toggleAnswers();

                    await this.question
                        .getAnswerListToggleText()
                        .should.eventually.be.equal(text, `Текст раскрывашки изменился на "${text}"`);

                    await this.question.answerList
                        .vndIsExisting()
                        .should.eventually.be.equal(false, 'Список ответов скрылся');
                },
            }),
        },
    },
});
