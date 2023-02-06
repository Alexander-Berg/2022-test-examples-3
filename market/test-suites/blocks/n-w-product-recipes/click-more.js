import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок n-popular-recipes.
 * @param {PageObject.PopularRecipes} popularRecipes
 */
export default makeSuite('Блок популярных рецептов. Кнопка "Показать ещё".', {
    feature: 'Блок перелинковки',
    story: {
        'При клике': {
            beforeEach() {
                return this.linker.clickShowMore();
            },

            'открывает все рецепты в правильной последовательности': makeCase({
                id: 'marketfront-1758',
                issue: 'MARKETVERSTKA-26399',
                params: {
                    recipes: 'Список названий рецептов',
                },
                async test() {
                    const links = await this.linker.getLinksName();

                    links.length.should.to.be.equal(
                        this.params.recipes.length,
                        `Кол-во ссылок увеличилось до ${this.params.recipes.length}`
                    );

                    return links.should.to.be.deep.equal(
                        this.params.recipes,
                        'Рецепты правильно отсортированы'
                    );
                },
            }),
        },
    },
});
