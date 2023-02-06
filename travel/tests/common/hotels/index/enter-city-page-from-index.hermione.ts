import {index} from 'suites/hotels';
import {assert} from 'chai';

import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';

describe(index.name, () => {
    it('Переход с рецептов на страницу города', async function () {
        const app = new TestHotelsApp(this.browser);

        await app.goToIndexPage();

        const {indexPage} = app;

        assert.isTrue(
            await indexPage.crossLinksGallery.isDisplayed(),
            'Должен отображаться блок Поиск отелей и других вариантов размещения',
        );

        const firstRecipe = await indexPage.crossLinksGallery.items.first();

        const firstRecipeTitle = await firstRecipe.title.getText();

        await firstRecipe.click();

        await this.browser.switchToNextTab();

        const {cityPage} = app;

        await cityPage.waitForLoadingFinished();

        assert.isTrue(
            await cityPage.cityPageBreadcrumps.waitForVisible(),
            'Должен присутствовать заголовок на странице города',
        );

        assert.equal(
            await cityPage.searchForm.place.getTriggerValue(),
            firstRecipeTitle,
            'Должен совпадать город из формы поиска с тем, что было на карточке',
        );
    });
});
