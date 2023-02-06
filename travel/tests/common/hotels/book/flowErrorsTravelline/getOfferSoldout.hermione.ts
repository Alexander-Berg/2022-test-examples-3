import {book} from 'suites/hotels';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

import {getOfferSoldoutTest} from '../errorFlow/base/getOfferSoldoutTest';

describe(book.name, () => {
    it('(ТЛ) Обработка солдаута при переходе на бронь', async function () {
        const tomorrow = moment().add(1, 'days');
        const dayAfterTomorrow = moment().add(2, 'days');

        const testOfferParams = getTestOfferParams({
            occupancy: '2',
            cancellation: 'CR_NON_REFUNDABLE',
            offerName: 'test',
            priceAmount: 11,
            priceMismatchRate: 1.1,
            getOfferOutcome: 'OO_SOLD_OUT',
            hotelDataLookupOutcome: 'HO_REAL',
            checkinDate: tomorrow.format(dateFormats.ROBOT),
            checkoutDate: dayAfterTomorrow.format(dateFormats.ROBOT),
        });

        await getOfferSoldoutTest(this.browser, testOfferParams, {
            hotelName: 'Вега Измайлово',
            checkinDate: tomorrow.format(
                dateFormats.HUMAN_DATE_WITH_SHORT_WEEKDAY,
            ),
            checkoutDate: dayAfterTomorrow.format(
                dateFormats.HUMAN_DATE_WITH_SHORT_WEEKDAY,
            ),
            guests: '2 взрослых',
            canCheckBedGroups: true,
            canCheckPartnerDescriptions: true,
            canCheckPartnerImages: true,
        });
    });
});
