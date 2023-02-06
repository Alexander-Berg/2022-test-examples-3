import {makeCase, makeSuite} from 'ginny';

/**
* Тесты на отображение данных в сниппете вопроса: аватар, имя пользователя, дата
* @property {PageObject.QuestionSnippet} this.questionSnippet
* @param {string} this.params.expectedUserName ожидаемое имя пользователя
* @param {string} this.params.expectedDate ожидаемая дата
*/
export default makeSuite('Сниппет вопроса.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'cниппет отображается': makeCase({
                id: 'marketfront-3689',
                feature: 'Контент страницы',
                test() {
                    return this.questionSnippet.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет вопроса отображается');
                },
            }),
            'аватарка в сниппете отображается': makeCase({
                id: 'marketfront-2918',
                issue: 'MARKETVERSTKA-31273',
                feature: 'Контент страницы',
                test() {
                    return this.questionSnippet.isAvatarVisible()
                        .should.eventually.be.equal(true, 'Аватар пользователя отображается');
                },
            }),
            'имя пользователя отображается': makeCase({
                id: 'marketfront-2919',
                issue: 'MARKETVERSTKA-31274',
                feature: 'Контент страницы',
                test() {
                    return this.questionSnippet.getUserNameText()
                        .should.eventually.be.equal(
                            this.params.expectedUserName,
                            'Имя пользователя отображается'
                        );
                },
            }),
            'дата публикации отображается': makeCase({
                id: 'marketfront-2920',
                issue: 'MARKETVERSTKA-31275',
                feature: 'Контент страницы',
                test() {
                    return this.questionSnippet.getDateText()
                        .should.eventually.be.equal(
                            this.params.expectedDate,
                            'Дата публикации корректная'
                        );
                },
            }),
        },
    },
});
