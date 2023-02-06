import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.UserQuestions} userQuestions
 */
export default makeSuite('Нет вопросов.', {
    story: {
        'Блок "Задайте вопрос"': {
            'по умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3109',
                    issue: 'MARKETFRONT-6439',
                    test() {
                        return this.userQuestions.isZeroStateVisible()
                            .should.eventually.to.be.equal(true, 'Блок "Задайте вопрос" виден');
                    },
                }),
            },
        },
    },
});
