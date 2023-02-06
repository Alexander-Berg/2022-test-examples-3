import {random} from 'lodash';
import {assert} from 'chai';
import moment from 'moment';
import {serp} from 'suites/hotels';

import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';
import {delay} from 'helpers/project/common/delay';

const countDaysBeforeCheckin = random(1, 7);
const countDaysBeforeCheckout = countDaysBeforeCheckin + random(1, 7);
const SEARCH_PARAMS = {
    place: 'Москва',
    geoId: 213,
    adults: 1,
    childrenAges: [3],
    checkinDate: moment()
        .add(countDaysBeforeCheckin, 'days')
        .format(dateFormats.ROBOT),
    checkoutDate: moment()
        .add(countDaysBeforeCheckout, 'days')
        .format(dateFormats.ROBOT),
};

describe(serp.name, () => {
    it('Пагинация', async function () {
        const app = new TestHotelsApp(this.browser);
        const hotelsSearchPage = app.searchPage;

        await app.goToSearchPage(SEARCH_PARAMS);

        await hotelsSearchPage.searchResults.status.waitLoading();

        await hotelsSearchPage.hotelsHeaderSearchInformation.searchInformation.isDisplayed();

        const searchFormInformation = await (hotelsSearchPage.isDesktop
            ? hotelsSearchPage.getSearchFormInformation()
            : hotelsSearchPage.getHeaderSearchInformation());

        const searchFormInformationByParams = hotelsSearchPage.isDesktop
            ? hotelsSearchPage.getSearchFormInformationByParams(SEARCH_PARAMS)
            : hotelsSearchPage.getHeaderSearchInformationByParams(
                  SEARCH_PARAMS,
              );

        assert.deepEqual(
            searchFormInformation,
            searchFormInformationByParams,
            'Название города, даты и кол-во гостей совпадают с выбранными на 1м шаге',
        );

        const initialHotelsCount = (
            await hotelsSearchPage.searchResults.hotelCards.items
        ).length;

        await hotelsSearchPage.footer.scrollIntoView();

        if (hotelsSearchPage.isTouch) {
            await delay(3000);

            const newHotelsCount = (
                await hotelsSearchPage.searchResults.hotelCards.items
            ).length;

            assert.isTrue(
                newHotelsCount > initialHotelsCount,
                'Загрузились еще несколько отелей',
            );

            return;
        }

        // 1-ая страница изначально
        {
            const isPrevButtonDisabled =
                await hotelsSearchPage.searchResults.prevButton.isDisabled();
            const isNextButtonDisabled =
                await hotelsSearchPage.searchResults.nextButton.isDisabled();

            assert.isTrue(
                isPrevButtonDisabled,
                'Кнопка Назад задизейблена на 1ой странице изначально',
            );

            assert.isFalse(
                isNextButtonDisabled,
                'Кнопка Далее кликабельна на 1ой странице изначально',
            );

            await hotelsSearchPage.searchResults.nextButton.click();
        }

        // 2-ая страница
        {
            await hotelsSearchPage.searchResults.status.waitLoading();

            await hotelsSearchPage.footer.scrollIntoView();

            const isPrevButtonDisabled =
                await hotelsSearchPage.searchResults.prevButton.isDisabled();
            const isNextButtonDisabled =
                await hotelsSearchPage.searchResults.nextButton.isDisabled();

            assert.isFalse(
                isPrevButtonDisabled,
                'Кнопка Назад задизейблена на 2ой странице',
            );

            assert.isFalse(
                isNextButtonDisabled,
                'Кнопка Далее кликабельна на 2ой странице',
            );

            await hotelsSearchPage.searchResults.prevButton.click();
        }

        // 1-ая страница после переходов
        {
            await hotelsSearchPage.searchResults.status.waitLoading();

            await hotelsSearchPage.footer.scrollIntoView();

            const isPrevButtonDisabled =
                await hotelsSearchPage.searchResults.prevButton.isDisabled();
            const isNextButtonDisabled =
                await hotelsSearchPage.searchResults.nextButton.isDisabled();

            assert.isTrue(
                isPrevButtonDisabled,
                'Кнопка Назад задизейблена на 1ой странице после переходов',
            );

            assert.isFalse(
                isNextButtonDisabled,
                'Кнопка Далее кликабельна на 1ой странице после переходов',
            );
        }
    });
});
