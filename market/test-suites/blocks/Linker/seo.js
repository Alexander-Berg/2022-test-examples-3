import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker|PageObject.PopularRecipes} linker
 * @param {PageObject.Headline} headline
 */
export default makeSuite('Блок перелинковки. Seo.', {
    feature: 'Блок перелинковки',
    story: {
        'При переходе по ссылке': {
            'заголовок, title и description соответствует рецепту': makeCase({
                id: 'marketfront-2872',
                issue: 'MARKETVERSTKA-29644',
                async test() {
                    const linkName = await this.linker.getItemTextByIndex(1);
                    await this.linker.clickItemByIndex(1);

                    return this.browser.allure.runStep(
                        'Проверяем, что в заголовке, title и description название рецепта',
                        async () => {
                            const titleHtml = await this.browser.getHTML('title');
                            const title = titleHtml.replace(/<[^>]+>/g, '');
                            const description = await this.browser.getAttribute('meta[name="description"]', 'content');
                            const matcher = `${linkName} — `;
                            const titleOnPageText = await this.title.getTitleText();

                            await this.expect(titleOnPageText)
                                .to.be.equal(linkName, 'Тайтл на странице соответствует рецепту');
                            await this.expect(title).to.include(matcher, 'Title соответствует рецепту');
                            await this.expect(description).to.include(matcher, 'Description соответствует рецепту');
                        }
                    );
                },
            }),
        },
    },
});
