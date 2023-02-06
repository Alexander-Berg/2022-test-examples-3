import {ITestAviaContextForm} from './types';
import {
    ECheckAvailabilityBeforeBookingOutcome,
    EConfirmationOutcome,
    EMqEventOutcome,
    ETokenizationOutcome,
    ECheckAvailabilityOnRedirOutcome,
} from 'server/api/AviaBookingApi/types/IAviaTestContextTokenApiParams';

const initialValues: ITestAviaContextForm = {
    checkAvailabilityOnRedirOutcome: ECheckAvailabilityOnRedirOutcome.SUCCESS,
    checkAvailabilityBeforeBookingOutcome:
        ECheckAvailabilityBeforeBookingOutcome.SUCCESS,
    tokenizationOutcome: ETokenizationOutcome.SUCCESS,
    confirmationOutcome: EConfirmationOutcome.SUCCESS,
    mqEventOutcome: EMqEventOutcome.NO_EVENT,
    skipPayment: true,
    aviaVariants: '',
};

export default initialValues;
