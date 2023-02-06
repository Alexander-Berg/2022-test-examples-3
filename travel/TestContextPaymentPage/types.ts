import {
    EPaymentFailureResponseCode,
    EPaymentOutcome,
} from 'server/api/OrdersAPI/types/TGetPaymentTestContextTokenParams';

export interface ITestPaymentContextForm {
    paymentOutcome: EPaymentOutcome;
    paymentFailureResponseCode: EPaymentFailureResponseCode;
    paymentFailureResponseDescription: string;
}
