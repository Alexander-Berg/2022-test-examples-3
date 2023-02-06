import {assert} from 'chai';
import moment from 'moment';
import {hotel} from 'suites/hotels';
import {random} from 'lodash';

import {SECOND} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import extractNumbers from 'helpers/utilities/extractNumbers';
import getHumanMonth from 'helpers/utilities/getHumanMonth';
import TestApp from 'helpers/project/TestApp';

interface ICheckData {
    adults?: number;
    hotelTitle: string;
    hotelHeaderTitle: string;
    checkinDate?: string;
    checkoutDate?: string;
}

describe(hotel.name, () => {
    it('Страница отеля без дат', async function () {
        const searchForNightsCount = 1;
        const checkinDate = moment().add(random(7, 21), 'days');
        const checkoutDate = moment(checkinDate).add(
            searchForNightsCount,
            'days',
        );
        const startHotelParams = {
            adults: 1,
            hotelPermalink: 1376154547,
            hotelTitle: 'Cosmos Moscow Vdnh Hotel 3',
            hotelHeaderTitle: 'Cosmos Moscow Vdnh Hotel',
        };

        const app = new TestApp(this.browser);
        const page = app.hotelsApp.hotelPage;

        await page.goToHotel(startHotelParams);

        await testSearchHotels(startHotelParams);

        const nextHotelsParams = {
            checkinDate: checkinDate.format(dateFormats.ROBOT),
            checkoutDate: checkoutDate.format(dateFormats.ROBOT),
            hotelTitle: 'Cosmos Moscow Vdnh Hotel 3',
            hotelHeaderTitle: 'Cosmos Moscow Vdnh Hotel',
        };

        await page.openSearchForm();
        await page.searchForm.fill(nextHotelsParams);
        await page.searchForm.submitForm();

        await this.browser.pause(SECOND);

        await page.state.waitForLoadingFinished();
        await testSearchHotels(nextHotelsParams);

        async function testSearchHotels(params: ICheckData): Promise<void> {
            assert.isTrue(
                await page.headerSearch.searchInformation.isVisible(),
                'На уровне хэдера должна отображаться шапка поиска',
            );

            const {date, direction} =
                await page.headerSearch.searchInformation.getSearchParams();

            assert.equal(
                direction,
                params.hotelHeaderTitle,
                `Название отеля в шапке ${params.hotelHeaderTitle} и запросе должны совпадать`,
            );

            if (!params.checkinDate) {
                assert.equal(
                    date,
                    'Выберите дату',
                    `Вместо даты в шапке должна быть надпись Выберите дату`,
                );
            }

            assert.equal(
                await page.hotelName.getText(),
                params.hotelTitle,
                `Под ссылкой должно отображаться название отеля ${params.hotelTitle}`,
            );

            await page.offersInfo.scrollIntoView();

            if (params.checkinDate) {
                const titleText =
                    await page.offersInfo.mainOffersTitle.getText();

                const numbersFromMainOffersTitle = extractNumbers(
                    await page.offersInfo.getText(),
                );

                assert.equal(
                    numbersFromMainOffersTitle[0],
                    checkinDate.date(),
                    `Число месяца заселения в заголовке должно соответствовать введенной дате ${checkinDate.date}, ${numbersFromMainOffersTitle[0]}`,
                );

                assert.include(
                    titleText,
                    getHumanMonth(checkinDate),
                    'Месяц заселения в заголовке должен соответствовать введенной дате',
                );

                assert.equal(
                    numbersFromMainOffersTitle[1],
                    checkoutDate.date(),
                    'Число месяца выселения в заголовке должно соответствовать введенной дате',
                );
                assert.include(
                    titleText,
                    getHumanMonth(checkoutDate),
                    'Месяц выселения в заголовке должен соответствовать введенной дате',
                );
            } else {
                const titleTextWithoutDatePage =
                    await page.offersInfo.offersTitle.getText();

                assert.equal(
                    titleTextWithoutDatePage,
                    'Наличие мест',
                    'В заголовке блока предложений должна быть надпись Наличие мест',
                );
            }
        }
    });
});
