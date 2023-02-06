import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок n-w-product-recipes-list.
 * @param {PageObject.ProductRecipesList} productRecipesList
 * @param {PageObject.Headline} headline
 * @param {PageObject.FilterList} filter
 */
export default makeSuite('Блок «Часто ищут» (выбор рецепта).', {
    story: {
        'При переходе по первой ссылке': {
            ['попадаем на выдачу, где заголовок совпадает с текстом кликнутой ссылки, ' +
                'выставляются правильные фильтры'
            ]: makeCase({
                id: 'marketfront-1759',
                issue: 'MARKETVERSTKA-26400',
                async test() {
                    const recipeText = await this.productRecipesList.getItemTextByIndex(1);
                    await this.productRecipesList.clickItemByIndex(1);
                    await this.browser.yaWaitForPageReady();

                    const headlineText = await this.title.getTitleText();

                    await this.browser.allure.runStep(
                        'Проверяем, что текст рецепта совпадает с заголовком',
                        () => this.expect(recipeText).to.be.equal(
                            headlineText,
                            'Текст рецепта должен совпадать с заголовком'
                        )
                    );

                    const isSelectedFilter =
                        await this.browser.isSelected(`[data-filter-id="${this.params.filterId}"] input`);

                    return this.browser.allure.runStep(
                        'Проверяем, что фильтр из рецепта выбран',
                        () => this.expect(isSelectedFilter).to.be.equal(true, 'Фильтр должен быть выбран')
                    );
                },
            }),
        },
    },
});
