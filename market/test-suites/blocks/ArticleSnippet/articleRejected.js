import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ArticleSnippetPopupContent} articleSnippetPopupContent
 * @param {PageObject.АrticleSnippet} articleSnippet
 */
export default makeSuite('Сниппет статьи с замечаниями.', {
    story: {
        'По умолчанию': {
            'содержит корректные пункты контекстного меню': makeCase({
                issue: 'MARKETVERSTKA-31035',
                id: 'marketfront-2857',
                feature: 'Мои статьи: контекстное меню',
                async test() {
                    await this.expect(this.articleSnippetPopupContent.listItemsCount())
                        .to.be.equal(1, 'Количество элементов меню верное');
                    const menuItems = await this.articleSnippetPopupContent.getListItemText();
                    return this.expect(menuItems).to.be.equal('Удалить', 'Первый пункт в сниппете верный');
                },
            }),
            'содержит корректную информацию даты обновления и бейдж с замечаниями': makeCase({
                issue: 'MARKETVERSTKA-31028',
                id: 'marketfront-2846',
                feature: 'Мои статьи: Бэйджи',
                async test() {
                    await this.expect(this.articleSnippet.getChangeStatusDateText())
                        .to.be.equal('31 октября 2018', 'Дата обновления верная');
                    return this.expect(this.articleSnippet.getModerationStatusBarText())
                        .to.be.equal('ЕСТЬ ЗАМЕЧАНИЯ РЕДАКТОРА', 'Текст бейджа статуса верный');
                },
            }),
        },
    },
});
