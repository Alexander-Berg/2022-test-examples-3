import {book} from 'suites/hotels';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

import {createOrderSoldout} from '../errorFlow/base/createOrderSoldout';

describe(book.name, () => {
    it('(ТЛ) Обработка события солдаут при создании брони', async function () {
        const tomorrow = moment().add(1, 'days');
        const dayAfterTomorrow = moment().add(2, 'days');

        const testOfferParams = getTestOfferParams({
            occupancy: '2',
            offerName: 'test',
            priceAmount: 11000,
            partiallyRefundRate: 50,
            createOrderOutcome: 'OO_SOLD_OUT',
            hotelDataLookupOutcome: 'HO_REAL',
            checkinDate: tomorrow.format(dateFormats.ROBOT),
            checkoutDate: dayAfterTomorrow.format(dateFormats.ROBOT),
        });

        await createOrderSoldout(this.browser, testOfferParams, {
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
