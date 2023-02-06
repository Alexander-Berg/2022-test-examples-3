import {makeCase, makeSuite} from 'ginny';
import NoQuestions from '@self/platform/spec/page-objects/components/Questions/NoQuestions';

/**
 * @param {PageObject.QuestionList} questionList
 */
export default makeSuite('Нет вопросов.', {
    story: {
        'Блок "Пока никто не задавал вопросов"': {
            beforeEach() {
                return this.setPageObjects({
                    noQuestions: () => this.createPageObject(NoQuestions),
                });
            },
            'не отображается': makeCase({
                id: 'm-touch-2979',
                issue: 'MARKETFRONT-3451',
                test() {
                    return this.noQuestions.isVisible()
                        .should
                        .eventually
                        .to
                        .be
                        .equal(true, 'Блок "Пока никто не задавал вопросов" отображается');
                },
            }),
        },
    },
});
