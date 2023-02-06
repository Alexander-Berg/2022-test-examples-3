import {ITestTrainsContextForm} from './types';
import {
    EConfirmReservationOutcome,
    ECreateReservationOutcome,
    EInsuranceCheckoutConfirmOutcome,
    EInsuranceCheckoutOutcome,
    EInsurancePricingOutcome,
    ERefundCheckoutOutcome,
    ERefundPricingOutcome,
} from 'server/api/TrainsBookingApi/types/ITrainsTestContextToken';

const initialValues: ITestTrainsContextForm = {
    insurancePricingOutcome: EInsurancePricingOutcome.SUCCESS,
    insuranceCheckoutOutcome: EInsuranceCheckoutOutcome.SUCCESS,
    insuranceCheckoutConfirmOutcome: EInsuranceCheckoutConfirmOutcome.SUCCESS,

    refundPricingOutcome: ERefundPricingOutcome.SUCCESS,
    refundCheckoutOutcome: ERefundCheckoutOutcome.SUCCESS,

    createReservationOutcome: ECreateReservationOutcome.SUCCESS,

    confirmReservationOutcome: EConfirmReservationOutcome.SUCCESS,

    officeReturnDelayInSeconds: 0,
    officeAcquireDelayInSeconds: 0,
    alwaysTimeoutAfterConfirmingInSeconds: 0,

    setOnlyForSecondTrain: false,
};

export default initialValues;
