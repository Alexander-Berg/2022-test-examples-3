import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.Caption} caption
 */
export default makeSuite('Форма добавления вопроса, каунтер вопросов', {
    feature: 'Структура страницы',
    story: {
        'При добавлении 1 вопроса': {
            'увеличится на 1': makeCase({
                id: 'marketfront-2867',
                issue: 'MARKETVERSTKA-31084',
                async test() {
                    await this.caption
                        .getQuestionsCount()
                        .should.eventually.be.equal('1', 'Количество вопросов равно 1');
                    await this.form.clickTextarea();
                    await this.form.setText('Мой новый вопрос');
                    await this.form
                        .getText()
                        .should.eventually.be.equal('Мой новый вопрос', 'Вопрос напечатан в поле ввода');
                    await this.form.clickSubmitButton();

                    // Дожидаемся срабатывания хендлеров JS и загрузки страницы
                    // нового вопроса
                    await this.browser.yaWaitForChangeUrl();

                    await this.browser.yaOpenPage(this.params.pageId, this.params.pageParams);
                    await this.caption
                        .getQuestionsCount()
                        .should.eventually.be.equal('2', 'Количество вопросов равно 2');
                },
            }),
        },
    },
});
