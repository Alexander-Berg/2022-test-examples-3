import _ from 'lodash';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок n-popular-recipes.
 * @param {PageObject.PopularRecipes} popularRecipes
 */
export default makeSuite('Блок популярных рецептов.', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'отображается на странице': makeCase({
                id: 'marketfront-1389',
                issue: 'MARKETVERSTKA-26381',
                params: {
                    recipesCountInitial: 'Количество изначально отображаемых рецептов',
                    recipesNames: 'Список названий рецептов',
                },
                test() {
                    const {recipesNames, recipesCountInitial} = this.params;

                    return this.popularRecipes.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок виден на странице')

                        .then(() => this.popularRecipes.getVisibleItemsNames())
                        .then(names => {
                            const guruRecipes = names
                                .filter(recipe => !(recipe.includes('по акции') || recipe.includes('по промокоду')));

                            const expectedRecipesNames = _.take(recipesNames, recipesCountInitial);

                            const recipesAssertsPromises = _.zip(guruRecipes, expectedRecipesNames)
                                .map(([actualRecipe, expectedRecipe]) =>
                                    this.expect(actualRecipe)
                                        .to.be.equal(expectedRecipe, 'Название рецепта правильное')
                                );

                            return Promise.all(recipesAssertsPromises);
                        });
                },
            }),
        },

        'Заголовок блока.': {
            'По умолчанию': {
                'содержит корректное значение': makeCase({
                    issue: 'MARKETVERSTKA-28997',
                    params: {
                        title: 'Заголовок блока',
                    },
                    test() {
                        return this.popularRecipes
                            .getTitleText()
                            .should.eventually.be.equal(this.params.title);
                    },
                }),
            },
        },

        'Кнопка "Показать ещё".': {
            'По умолчанию': {
                'видна': makeCase({
                    id: 'marketfront-1389',
                    issue: 'MARKETVERSTKA-26381',
                    test() {
                        return this.popularRecipes.showMore.isVisible()
                            .should.eventually.to.be.equal(true, 'Кнопка "Показать ещё" видна');
                    },
                }),

                'при клике': {
                    beforeEach() {
                        return this.popularRecipes.clickShowMore();
                    },

                    'открывает все рецепты': makeCase({
                        id: 'marketfront-1389',
                        issue: 'MARKETVERSTKA-26381',
                        params: {
                            recipesCountMax: 'Максимальное количество отображаемых рецептов',
                            recipesNames: 'Список названий рецептов',
                        },
                        test() {
                            const {recipesNames, recipesCountMax} = this.params;

                            return this.popularRecipes.getVisibleItemsNames()
                                .then(names => {
                                    const guruRecipes = names.filter(recipe =>
                                        !(recipe.includes('по акции') || recipe.includes('по промокоду'))
                                    );

                                    const expectedRecipesNames = _.take(recipesNames, recipesCountMax);

                                    const recipesAssertsPromises = _.zip(guruRecipes, expectedRecipesNames)
                                        .map(
                                            ([actualRecipe, expectedRecipe]) => this.expect(actualRecipe)
                                                .to.be.equal(expectedRecipe, 'Название рецепта правильное')
                                        );

                                    return Promise.all(recipesAssertsPromises);
                                });
                        },
                    }),
                },
            },
        },
    },
});
