import {assert} from 'chai';
import {hotel} from 'suites/hotels';
import moment from 'moment';
import {random} from 'lodash';

import {TestHotelPage} from 'helpers/project/hotels/pages/HotelPage/TestHotelPage';
import dateFormats from 'helpers/utilities/date/formats';
import {getRandomHotel} from 'helpers/project/hotels/data/hotels';

describe(hotel.name, () => {
    it('Общий вид страницы отеля без румов', async function () {
        const SEARCH_FOR_NIGHTS_COUNT = 1;
        const checkinDate = moment().add(random(7, 21), 'days');
        const checkoutDate = moment(checkinDate).add(
            SEARCH_FOR_NIGHTS_COUNT,
            'days',
        );

        const HOTEL_PARAMS = {
            adults: 1,
            checkinDate: checkinDate.format(dateFormats.ROBOT),
            checkoutDate: checkoutDate.format(dateFormats.ROBOT),
            hotelPermalink: getRandomHotel().permalink,
            srcParams: 'travel:enablePermarooms=false',
        };

        const page = new TestHotelPage(this.browser);

        await page.goToHotel(HOTEL_PARAMS);

        // Далее отображается страница отеля со следующими составляющими:
        await page.state.waitForLoadingFinished();

        const offers = page.offersInfo.mainOffers.offers;

        assert.isAbove(
            await offers.count(),
            0,
            'Должны отображаться офферы Яндекса',
        );

        await offers.forEach(async mainOffer => {
            assert.isTrue(
                await mainOffer.offerName.isVisible(),
                'Должно отображаться название оффера',
            );

            assert.isTrue(
                (await mainOffer.hotelOfferLabels.offerMealInfo.isVisible()) ||
                    (await mainOffer.hotelOfferLabels.hotelsCancellationInfo.trigger.isVisible()),
                'Ожидалась информация о питании и отменяемости',
            );
            assert.isTrue(
                await mainOffer.nightsCount.isVisible(),
                'Должно отображаться кол-во ночей',
            );
            assert.isTrue(
                await mainOffer.plusInfo.isVisible(),
                'Ожидалось отображение кол-ва баллов плюса',
            );
            assert.isTrue(
                await mainOffer.bookButton.isVisible(),
                'Должна быть кнопка бронирования у оффера',
            );
        });

        const partnerOffers = page.offersInfo.partnerOffers.offers;
        const countPartnerOffers =
            await page.offersInfo.partnerOffers.offers.count();

        assert.isAbove(
            await countPartnerOffers,
            0,
            'Ожидались офферы партнёров',
        );

        await partnerOffers.forEach(async offer => {
            assert.isTrue(
                await offer.hotelOperator.icon.isVisible(),
                'Ожидался логотип партнёра',
            );
            assert.isTrue(
                await offer.hotelOperator.name.isVisible(),
                'Ожидалось название партнёра',
            );
            assert.isTrue(
                (await offer.offerLabels.mealInfo.isVisible()) ||
                    (await offer.offerLabels.cancellationInfo.isVisible()),
                'Ожидалась информация о питании или отменяемости',
            );
            assert.isTrue(
                await offer.price.isVisible(),
                'Должна отображаться цена офера',
            );
            assert.isTrue(
                await offer.nightsCount.isVisible(),
                'Должно отображаться кол-во ночей офера',
            );
            assert.isTrue(
                await offer.bookButton.isVisible(),
                'Должна быть кнопка перехода к партнеру',
            );
        });
    });
});
