import {cityPage as cityPageSuit} from 'suites/hotels';
import {assert} from 'chai';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';

describe(cityPageSuit.name, () => {
    it('Переход на поиск со страницы города', async function () {
        const app = new TestHotelsApp(this.browser);

        await app.goToIndexPage();

        const {indexPage} = app;

        assert.isTrue(
            await indexPage.crossLinksGallery.isDisplayed(),
            'Должен отображаться блок Поиск отелей и других вариантов размещения',
        );

        const firstRecipe = await indexPage.crossLinksGallery.items.first();

        await firstRecipe.click();

        await this.browser.switchToNextTab();

        const {cityPage} = app;

        await cityPage.waitForLoadingFinished();

        assert.isTrue(
            await cityPage.cityPageBreadcrumps.waitForVisible(),
            'Должны присутствовать хлебные крошки на странице города',
        );

        const city = await cityPage.searchForm.place.getTriggerValue();

        const startDate = moment().add(5, 'day');
        const startDateRobot = startDate.format(dateFormats.ROBOT);
        const startDateHuman = startDate.format(dateFormats.HUMAN);
        const endDate = moment().add(6, 'day');
        const endDateRobot = endDate.format(dateFormats.ROBOT);
        const endDateHuman = endDate.format(dateFormats.HUMAN);

        await cityPage.searchForm.period.selectStartDate(startDateRobot);
        await cityPage.searchForm.period.selectEndDate(endDateRobot);
        await cityPage.searchForm.travellers.trigger.click();
        await cityPage.searchForm.travellers.adultsCount.setCount(1);

        if (cityPage.isTouch) {
            await cityPage.searchForm.travellers.modal.clickCompleteButton();
        }

        await cityPage.searchForm.submitForm();

        const {searchPage} = app;

        if (searchPage.isTouch)
            await searchPage.hotelsHeaderSearchInformation.openSearchForm();

        assert.equal(
            await searchPage.searchForm.place.getTriggerValue(),
            city,
            'Должен быть указан корректный город в поисковой форме',
        );

        assert.include(
            startDateHuman,
            await searchPage.searchForm.period.startTrigger.value.getText(),
            'Должны быть указана корректная дата заезда в поисковой форме',
        );

        assert.include(
            endDateHuman,
            await searchPage.searchForm.period.endTrigger.value.getText(),
            'Должны быть указана корректная дата выезда в поисковой форме',
        );

        await searchPage.searchForm.travellers.click();

        assert.equal(
            await searchPage.searchForm.travellers.adultsCount.count.getText(),
            '1',
            'Должно быть указано корректное количество гостей в форме поиска',
        );
    });
});
