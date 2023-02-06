import {index} from 'suites/avia';
import {assert} from 'chai';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';

describe(index.name, () => {
    it('Проверка перехода с рецептов', async function () {
        const app = new TestAviaApp(this.browser);

        await app.goToIndexPage();

        const {indexPage} = app;

        assert.isTrue(
            await indexPage.crossLinksGallery.isDisplayed(),
            'Должен отображаться блок Дешёвые авиабилеты на популярные направления',
        );

        const firstCrossLinkItem =
            await indexPage.crossLinksGallery.items.first();

        const firstRecipeTo = await firstCrossLinkItem.getTo();

        await firstCrossLinkItem.click();

        await this.browser.switchToNextTab();

        const {directionPage} = app;

        assert.isTrue(
            await directionPage.directionPageBreadcrumps.waitForVisible(),
            'Должны присутствовать хлебные крошки на странице направления',
        );

        assert.equal(
            await directionPage.searchForm.toSuggest.getTriggerValue(),
            firstRecipeTo,
            'Должен совпадать город из формы поиска с тем, что было на карточке',
        );

        if (directionPage.isDesktop) {
            assert.isTrue(
                await directionPage.searchForm.periodDatePicker.calendar.isDisplayed(),
                'Должен быть открыт календарь для выбора даты',
            );
        }
        //Для тачей сейчас есть баг с календарём: https://st.yandex-team.ru/TRAVELFRONT-6199 - нужно будет дописать тест, когда его поправят
    });
});
