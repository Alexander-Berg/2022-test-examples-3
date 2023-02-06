import moment from 'moment-timezone';

import {ITestHotelsContextForm} from './types';
import EHotelPartnerId from 'server/api/GenericOrderApi/types/common/service/IHotelServiceInfo/IHotelOfferInfo/EHotelPartnerId';
import {
    ECancellation,
    EConfirmationOutcome,
    EHotelDataLookupOutcome,
    EOutcome,
    EPansionType,
    ERefundOutcome,
    EReservationOutcome,
} from 'server/api/HotelsBookAPI/types/ITestBookOfferToken';

import {ROBOT} from 'utilities/dateUtils/formats';

const initialValues: ITestHotelsContextForm = {
    originalId: 742,
    partnerId: EHotelPartnerId.PI_TRAVELLINE,
    checkinDate: moment().add(1, 'day').format(ROBOT),
    checkoutDate: moment().add(3, 'day').format(ROBOT),
    occupancy: '2',
    forceAvailability: false,
    cancellation: ECancellation.FULLY_REFUNDABLE,
    pansionType: EPansionType.BD,
    offerName: '',
    priceAmount: 0,
    partiallyRefundRate: 0,
    partiallyRefundableInMinutes: 0,
    nonRefundableInMinutes: 0,
    getOfferOutcome: EOutcome.SUCCESS,
    createOrderOutcome: EOutcome.SUCCESS,
    hotelDataLookupOutcome: EHotelDataLookupOutcome.REAL,
    reservationOutcome: EReservationOutcome.SUCCESS,
    confirmationOutcome: EConfirmationOutcome.SUCCESS,
    refundOutcome: ERefundOutcome.SUCCESS,
    priceMismatchRate: 1.1,
    existingDolphinOrder: '',
    ignorePaymentScheduleRestrictions: undefined,
    discountAmount: 0,
    mealPrice: 0,
    isPostPay: false,
};

export default initialValues;
