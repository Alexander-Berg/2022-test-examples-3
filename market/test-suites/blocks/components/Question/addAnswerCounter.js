import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.Caption} caption
 */
export default makeSuite('Форма добавления ответа, каунтер ответов', {
    feature: 'Структура страницы',
    story: {
        'При добавлении 1 ответа': {
            'увеличится на 1': makeCase({
                id: 'marketfront-2916',
                issue: 'MARKETVERSTKA-31264',
                async test() {
                    await this.answersList.getAnswersCountFromHeader()
                        .should.eventually.be.equal(1, 'Количество ответов равно 1');
                    await this.form.clickTextarea();
                    await this.form.setText('В них можно слушать русский рэп');
                    await this.form.clickSubmitButton();

                    await this.inlineNotification.waitForVisible();

                    await this.answersList.getAnswersCountFromHeader()
                        .should.eventually.be.equal(2, 'Количество ответов равно 2');
                },
            }),
        },
    },
});
