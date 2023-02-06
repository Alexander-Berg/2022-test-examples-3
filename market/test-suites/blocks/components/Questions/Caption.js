import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Caption} caption
 * @param {PageObject.Remove} remove
 * @param {PageObject.Dialog} dialog
 */
export default makeSuite('Заголовок «Все вопросы о товаре»', {
    feature: 'Структура страницы',
    story: {
        'При удалении вопроса': {
            'счётчик уменьшается на 1': makeCase({
                id: 'marketfront-2868',
                issue: 'MARKETVERSTKA-31085',
                async test() {
                    await this.caption
                        .getQuestionsCount()
                        .should.eventually.be.equal('2', 'Количество вопросов равно 2');
                    await this.remove.click();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickSubmitButton();
                    await this.dialog.waitForContentHidden();
                    await this.caption
                        .getQuestionsCount()
                        .should.eventually.be.equal('1', 'Количество вопросов равно 1');
                },
            }),
        },
    },
});
