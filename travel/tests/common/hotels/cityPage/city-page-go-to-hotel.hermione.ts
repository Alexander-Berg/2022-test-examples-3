import {cityPage as cityPageSuit} from 'suites/hotels';
import {assert} from 'chai';
import moment from 'moment';

import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';
import dateFormats from 'helpers/utilities/date/formats';

describe(cityPageSuit.name, () => {
    it('Переход на страницу отеля со страницы города', async function () {
        const checkinDate = moment().add(5, 'day');
        const checkoutDate = moment().add(6, 'day');

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

        if (cityPage.isDesktop) {
            await cityPage.searchForm.place.click();
        }

        const firstVariant = await cityPage.searchResults.hotelCards.first();

        await firstVariant.scrollIntoView();
        await firstVariant.buyButton.click();
        await cityPage.fillSearchParametersInHotelModalAndSubmit(
            checkinDate,
            checkoutDate,
            1,
        );

        const {hotelPage} = app;

        await hotelPage.state.waitForLoadingFinished();
        await hotelPage.openSearchForm();

        if (hotelPage.isDesktop) {
            assert.include(
                checkinDate.format(dateFormats.HUMAN_SHORT),
                await hotelPage.searchForm.period.startTrigger.value.getText(),
                'Должна совпадать дата заселения',
            );

            assert.include(
                checkoutDate.format(dateFormats.HUMAN_SHORT),
                await hotelPage.searchForm.period.endTrigger.value.getText(),
                'Должна совпадать дата выселения',
            );
        } else {
            assert.equal(
                await hotelPage.searchForm.period.startTrigger.value.getText(),
                checkinDate.format(dateFormats.HUMAN),
                'Должна совпадать дата заселения',
            );

            assert.equal(
                await hotelPage.searchForm.period.endTrigger.value.getText(),
                checkoutDate.format(dateFormats.HUMAN),
                'Должна совпадать дата выселения',
            );
        }

        await hotelPage.searchForm.travellers.trigger.click();

        assert.equal(
            await hotelPage.searchForm.travellers.adultsCount.count.getHTML(),
            '1',
            'Должно совпадать количество гостей',
        );
    });
});
