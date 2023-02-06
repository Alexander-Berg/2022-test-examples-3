import {assert} from 'chai';
import {trip} from 'suites/trips';

import TestApp from 'helpers/project/TestApp';
import dateFormats from 'helpers/utilities/date/formats';
import humanizePeriod from 'helpers/utilities/date/humanizePeriod';

describe(trip.name, () => {
    it('Блок отельного кросс-сейла в моей поездке', async function () {
        const app = new TestApp(this.browser);

        // do: Авторизоваться
        await app.loginRandomAccount();

        const {
            accountApp,
            accountApp: {tripPage},
            hotelsApp: {searchPage: hotelsSearchPage},
        } = app;

        // do: Включить фронтовые моки бекенда (cookie "use_trips_mock" со значением "true")
        await accountApp.useTripsApiMock();

        // do: Зайти на страницу "/my/trips/1"
        await tripPage.goToTrip();

        const {hotelsCrossSaleBlock} = tripPage;

        // do: Проскроллить до блока кросс-сейла
        await hotelsCrossSaleBlock.scrollIntoView();

        await hotelsCrossSaleBlock.waitForLoading();

        const {checkinDate, checkoutDate} =
            await hotelsCrossSaleBlock.parseDates();

        // do: Кликнуть по блоку
        await hotelsCrossSaleBlock.map.searchLink.click();

        await this.browser.switchToNextTab();

        assert.equal(
            await app.getPagePathname(),
            '/hotels/search/',
            'Произошел переход на выдачу отеля - "/hotels/search"',
        );

        let areDatesCorrect: boolean;

        if (app.isDesktop) {
            const searchFormInformation =
                await hotelsSearchPage.getSearchFormInformation();

            const isCheckinDateCorrect =
                searchFormInformation.checkinDate ===
                checkinDate.format(dateFormats.HUMAN_SHORT);
            const isCheckoutDateCorrect =
                searchFormInformation.checkoutDate ===
                checkoutDate.format(dateFormats.HUMAN_SHORT);

            areDatesCorrect = isCheckinDateCorrect && isCheckoutDateCorrect;
        } else {
            const searchFormInformation =
                await hotelsSearchPage.getHeaderSearchInformation();

            const date = humanizePeriod(
                checkinDate.format(dateFormats.ROBOT),
                checkoutDate.format(dateFormats.ROBOT),
                true,
            );

            areDatesCorrect = date === searchFormInformation.date;
        }

        assert.isTrue(areDatesCorrect, 'Даты соответствуют датам из поездки');

        // do: Вернуться в поездку
        await this.browser.switchToPreviousTab();

        // do: Нажать крестик у кроссейл блока
        await hotelsCrossSaleBlock.closeIcon.click();

        assert.isFalse(
            await hotelsCrossSaleBlock.isVisible(),
            'Блок кросс-сейла скрылся',
        );

        // do: Обновить страницу
        await this.browser.refresh();

        assert.isFalse(
            await hotelsCrossSaleBlock.isVisible(),
            'После перезагрузки блок не показывается',
        );
    });
});
