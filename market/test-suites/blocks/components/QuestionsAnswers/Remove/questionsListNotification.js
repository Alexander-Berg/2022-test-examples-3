import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Remove} remove
 * @param {PageObject.Dialog} dialog
 * @param {PageObject.Notification} notification
 */
export default makeSuite('Кнопка «Удалить», нотификация', {
    feature: 'Удаление вопроса',
    story: {
        'По умолчанию': {
            'после удаления вопроса отображается нотификация «Вопрос удалён»': makeCase({
                id: 'marketfront-2854',
                issue: 'MARKETVERSTKA-31060',
                async test() {
                    await this.remove.click();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickSubmitButton();
                    await this.dialog.waitForContentHidden();
                    await this.notification
                        .getText()
                        .should.eventually.be.equal('Вопрос удалён');
                },
            }),
        },
    },
});
