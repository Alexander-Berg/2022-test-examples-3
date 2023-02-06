import {ITestPaymentContextForm} from './types';
import {
    EPaymentFailureResponseCode,
    EPaymentOutcome,
} from 'server/api/OrdersAPI/types/TGetPaymentTestContextTokenParams';

const initialValues: ITestPaymentContextForm = {
    paymentOutcome: EPaymentOutcome.PO_SUCCESS,
    paymentFailureResponseCode:
        EPaymentFailureResponseCode.AUTHORIZATION_REJECT,
    paymentFailureResponseDescription: 'что-то пошло не так',
};

export default initialValues;
