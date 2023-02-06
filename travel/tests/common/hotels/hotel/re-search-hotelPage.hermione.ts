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
    adults: number;
    hotelTitle: string;
    hotelHeaderTitle: string;
}

describe(hotel.name, () => {
    it('Перепоиск на странице отеля', async function () {
        const searchForNightsCount = 1;
        const checkinDate = moment().add(random(7, 21), 'days');
        const checkoutDate = moment(checkinDate).add(
            searchForNightsCount,
            'days',
        );
        const startHotelParams = {
            adults: 1,
            checkinDate: checkinDate.format(dateFormats.ROBOT),
            checkoutDate: checkoutDate.format(dateFormats.ROBOT),
            hotelPermalink: 1152255963,
            hotelTitle: 'Radisson Collection Hotel, Moscow 5',
            hotelHeaderTitle: 'Radisson Collection Hotel, Moscow',
        };

        const app = new TestApp(this.browser);
        const page = app.hotelsApp.hotelPage;

        await page.goToHotel(startHotelParams);

        await testSearchHotels(startHotelParams);

        await page.openSearchForm();

        const nextHotelsParams = {
            adults: 2,
            checkinDate: checkinDate.format(dateFormats.ROBOT),
            checkoutDate: checkoutDate.format(dateFormats.ROBOT),
            place: 'Novotel Москва Сити',
            hotelTitle: 'Novotel Москва Сити 4',
            hotelHeaderTitle: 'Novotel Москва Сити',
        };

        await page.searchForm.fill(nextHotelsParams);
        await page.searchForm.submitForm();

        await this.browser.pause(SECOND);

        await page.state.waitForLoadingFinished();
        await testSearchHotels(nextHotelsParams);

        async function testSearchHotels(params: ICheckData): Promise<void> {
            await page.state.waitForLoadingFinished();

            assert.isTrue(
                await page.headerSearch.searchInformation.isVisible(),
                'На уровне хэдера должна отображаться шапка поиска',
            );

            const {direction} =
                await page.headerSearch.searchInformation.getSearchParams();

            assert.equal(
                direction,
                params.hotelHeaderTitle,
                `Название отеля в шапке ${params.hotelHeaderTitle} и запросе должны совпадать`,
            );

            await page.offersInfo.scrollIntoView();

            const hasMainOffers =
                await page.offersInfo.mainOffersTitle.isVisible();

            if (hasMainOffers) {
                assert.isTrue(
                    await page.offersInfo.mainOffersTitle.isVisible(),
                    'В вверху блока должны быть указаны даты и кол-во гостей',
                );
            } else {
                assert.isTrue(
                    await page.offersInfo.partnerOffersTitle.isVisible(),
                    'В вверху блока должны быть указаны даты и кол-во гостей',
                );
            }

            const titleTextQa = hasMainOffers
                ? await page.offersInfo.mainOffersTitle.getText()
                : await page.offersInfo.partnerOffersTitle.getText();

            const numbersFromMainOffersTitle = extractNumbers(
                await page.offersInfo.getText(),
            );

            assert.equal(
                numbersFromMainOffersTitle[0],
                checkinDate.date(),
                `Число месяца заселения в заголовке должно соответствовать дате на первом шаге кейса ${checkinDate.date}, ${numbersFromMainOffersTitle[0]}`,
            );
            assert.include(
                titleTextQa,
                getHumanMonth(checkinDate),
                'Месяц заселения в заголовке должен соответствовать дате на первом шаге кейса',
            );

            assert.equal(
                numbersFromMainOffersTitle[1],
                checkoutDate.date(),
                'Число месяца выселения в заголовке должно соответствовать дате на первом шаге кейса',
            );
            assert.include(
                titleTextQa,
                getHumanMonth(checkoutDate),
                'Месяц выселения в заголовке должен соответствовать дате на первом шаге кейса',
            );

            assert.equal(
                await page.hotelName.getText(),
                params.hotelTitle,
                `Под ссылкой должно отображаться название отеля ${params.hotelTitle}`,
            );

            await page.offersInfo.scrollIntoView();

            assert.isTrue(
                await page.offersInfo.isVisible(),
                'Ниже должен отображаться блок номеров',
            );

            if (hasMainOffers) {
                assert.isTrue(
                    await page.offersInfo.mainOffersTitle.isVisible(),
                    'В вверху блока должны быть указаны даты и кол-во гостей',
                );
            } else {
                assert.isTrue(
                    await page.offersInfo.partnerOffersTitle.isVisible(),
                    'В вверху блока должны быть указаны даты и кол-во гостей',
                );
            }

            assert.equal(
                numbersFromMainOffersTitle[0],
                checkinDate.date(),
                'Число месяца заселения в заголовке должно соответствовать дате на первом шаге кейса',
            );
            assert.include(
                titleTextQa,
                getHumanMonth(checkinDate),
                'Месяц заселения в заголовке должен соответствовать дате на первом шаге кейса',
            );

            assert.equal(
                numbersFromMainOffersTitle[1],
                checkoutDate.date(),
                'Число месяца выселения в заголовке должно соответствовать соответствуют дате на первом шаге кейса',
            );
            assert.include(
                titleTextQa,
                getHumanMonth(checkoutDate),
                'Месяц выселения в заголовке должен соответствовать дате на первом шаге кейса',
            );

            const searchOnNextYear = numbersFromMainOffersTitle[2] > 2000;

            assert.equal(
                searchOnNextYear
                    ? numbersFromMainOffersTitle[3]
                    : numbersFromMainOffersTitle[2],
                params.adults,
                'Кол-во гостей в заголовке должны соответствовать кол-ву на первом шаге кейса',
            );

            assert.isTrue(
                await page.offersInfo.hotelPageSearchForm.isVisible(),
                'Ниже должна отображаться форма поиска',
            );
            assert.match(
                await page.offersInfo.hotelPageSearchForm.period.startTrigger.getText(),
                new RegExp(`${checkinDate.date()} [А-Яа-я]+`),
                'Дата заезда в форме должна соответствовать введенным данным на первом шаге кейса',
            );
            assert.match(
                await page.offersInfo.hotelPageSearchForm.period.endTrigger.getText(),
                new RegExp(`${checkoutDate.date()} [А-Яа-я]+`),
                'Дата выезда в форме должна соответствовать введенным данным на первом шаге кейса',
            );
            assert.match(
                await page.offersInfo.hotelPageSearchForm.travellers.trigger.getText(),
                new RegExp(`${params.adults} [А-Яа-я]+`),
                'Кол-во гостей в форме должно соответствовать введенным данным на первом шаге кейса',
            );
        }
    });
});
