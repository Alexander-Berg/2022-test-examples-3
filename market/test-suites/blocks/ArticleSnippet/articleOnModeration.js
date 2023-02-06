import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ArticleSnippetPopupContent} articleSnippetPopupContent
 */
export default makeSuite('Сниппет статьи на модерации.', {
    story: {
        'Контент попапа': makeCase({
            issue: 'MARKETVERSTKA-31034',
            id: 'marketfront-2856',
            feature: 'Мои статьи: контекстное меню',
            async test() {
                await this.expect(this.articleSnippetPopupContent.listItemsCount())
                    .to.be.equal(1, 'Количество элементов меню верное');
                return this.expect(this.articleSnippetPopupContent.getListItemText())
                    .to.be.equal('Удалить', 'Текст в сниппете верный');
            },
        }),
        'Контент снипета': makeCase({
            issue: 'MARKETVERSTKA-31027',
            id: 'marketfront-2845',
            feature: 'Мои статьи: Бэйджи',
            async test() {
                await this.expect(this.articleSnippet.getChangeStatusDateText())
                    .to.be.equal('31 октября 2018', 'Дата обновления верная');
                return this.expect(this.articleSnippet.getModerationStatusBarText())
                    .to.be.equal('НА МОДЕРАЦИИ', 'Текст бейджа статуса верный');
            },
        }),
    },
});
