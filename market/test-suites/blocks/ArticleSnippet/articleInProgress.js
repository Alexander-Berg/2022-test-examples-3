import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.АrticleSnippet} articleSnippet
 * @param {PageObject.ArticleSnippetPopupContent} articleSnippetPopupContent
 * @param {PageObject.dialog} Dialog
 * @param {PageObject.articlesGrid} ArticlesGrid
 */
export default makeSuite('Сниппет статьи которая находится в написании.', {
    story: {
        'По умолчанию': {
            'отображает дату и не отображает бейдж': makeCase({
                issue: 'MARKETVERSTKA-31026',
                id: 'marketfront-2843',
                feature: 'Мои статьи: Бэйджи',
                async test() {
                    await this.expect(this.articleSnippet.isChangeStatusDateVisible())
                        .to.be.equal(true, 'Дата обновления отображается');
                    return this.expect(this.articleSnippet.isModerationStatusBarVisible())
                        .to.be.equal(false, 'Текст бейджа статуса не отображается');
                },
            }),
        },
        'При клике по кнопке удаления и подтверждении удаления': {
            'статья удаляется': makeCase({
                issue: 'MARKETVERSTKA-31032',
                id: 'marketfront-2850',
                feature: 'Мои статьи: контекстное меню',
                async test() {
                    const snippetCount = await this.articlesGrid.articleSnippetCount();
                    await this.expect(snippetCount)
                        .to.be.equal(
                            1,
                            'Количество сниппетов сниппетов до удаления 1'
                        );
                    await this.articleSnippetPopupContent.clickRemoveLink();
                    await this.dialog.clickSubmitButton();
                    await this.dialog.waitForContentHidden();

                    let count = await this.articlesGrid.articleSnippetCount();
                    await this.expect(count)
                        .to.be.equal(
                            0,
                            'Количество сниппетов сниппетов после удаления 0'
                        );

                    await this.browser.refresh();
                    count = await this.articlesGrid.articleSnippetCount();
                    await this.expect(count)
                        .to.be.equal(
                            0,
                            'Количество сниппетов сниппетов после удаления 0'
                        );
                },
            }),
        },
    },
});
