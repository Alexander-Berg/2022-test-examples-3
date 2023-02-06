import moment from 'moment';

import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';

import dateFormats from 'helpers/utilities/date/formats';

export const TEST_OFFER_PARAMS: IBookOfferRequestParams = {
    originalId: 742,
    partnerId: 'PI_TRAVELLINE',
    occupancy: '1',
    forceAvailability: true,
    cancellation: 'CR_FULLY_REFUNDABLE',
    pansionType: 'PT_BB',
    offerName: 'TEST_OFFER_NAME',
    checkinDate: moment().add(11, 'days').format(dateFormats.ROBOT),
    checkoutDate: moment().add(12, 'days').format(dateFormats.ROBOT),
    priceAmount: 5001,
    priceMismatchRate: 1,
    getOfferOutcome: 'OO_SUCCESS',
    createOrderOutcome: 'OO_SUCCESS',
    hotelDataLookupOutcome: 'HO_MOCKED',
    reservationOutcome: 'RO_SUCCESS',
    confirmationOutcome: 'CO_SUCCESS',
    refundOutcome: 'RF_SUCCESS',
};
