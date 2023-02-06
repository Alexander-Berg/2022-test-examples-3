import {book} from 'suites/hotels';
import moment from 'moment';

import {MINUTE} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

import {confirmationNotFound} from '../errorFlow/base/confirmationNotFound';

describe(book.name, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('(Bnovo) Обработка ошибки при подтверждении брони', async function () {
        const tomorrow = moment().add(1, 'days');
        const dayAfterTomorrow = moment().add(2, 'days');

        const testOfferParams = getTestOfferParams({
            originalId: 1221,
            partnerId: 'PI_BNOVO',
            occupancy: '2',
            cancellation: 'CR_NON_REFUNDABLE',
            offerName: 'test',
            checkinDate: tomorrow.format(dateFormats.ROBOT),
            checkoutDate: dayAfterTomorrow.format(dateFormats.ROBOT),
            priceAmount: 1000,
            priceMismatchRate: 1.1,
            hotelDataLookupOutcome: 'HO_REAL',
            confirmationOutcome: 'CO_NOT_FOUND',
        });

        await confirmationNotFound(this.browser, testOfferParams, {
            hotelName: 'Амай',
            checkinDate: tomorrow.format(
                dateFormats.HUMAN_DATE_WITH_SHORT_WEEKDAY,
            ),
            checkoutDate: dayAfterTomorrow.format(
                dateFormats.HUMAN_DATE_WITH_SHORT_WEEKDAY,
            ),
            guests: '2 взрослых',
            canCheckBedGroups: false,
            canCheckPartnerDescriptions: false,
            canCheckPartnerImages: false,
        });
    });
});
