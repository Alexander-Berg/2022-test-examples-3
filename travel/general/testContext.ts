import {IGetPaymentTestContextTokenParams} from 'helpers/project/common/api/types/PaymentTestContext';
import {IAviaTestContextParams} from 'helpers/project/avia/api/types/AviaTestContext';
import {ITrainsTestContextTokenParams} from 'helpers/project/trains/api/types/ITrainsTestContextToken';

export const PAYMENT_TEST_CONTEXT = 'paymentTestContextToken';
export const START_PAYMENT_TEST_CONTEXT = 'startPaymentTestContextToken';

// paymentUrl добавлен потому что без paymentUrl не показывается на фронте никакой интерфейс на странице оплаты
export const MOCK_PAYMENT_URL = 'https://travel-test.yandex.ru';

export const TRAIN_TEST_CONTEXT = 'trainTestContextToken';

export const SUCCESS_PAYMENT_CONTEXT_PARAMS: IGetPaymentTestContextTokenParams =
    {
        paymentOutcome: 'PO_SUCCESS',
    };

export const TRAINS_SUCCESS_TEST_CONTEXT_PARAMS: ITrainsTestContextTokenParams =
    {
        insurancePricingOutcome: 'IPO_SUCCESS',
        insuranceCheckoutOutcome: 'ICO_SUCCESS',
        insuranceCheckoutConfirmOutcome: 'ICCO_SUCCESS',
        refundPricingOutcome: 'RPO_SUCCESS',
        refundCheckoutOutcome: 'RCO_SUCCESS',
        createReservationOutcome: 'RCRO_SUCCESS',
        confirmReservationOutcome: 'RCOO_SUCCESS',
    };

export const AVIA_SUCCESS_TEST_CONTEXT_PARAMS: IAviaTestContextParams = {
    checkAvailabilityOnRedirOutcome: 'CAOR_SUCCESS',
    checkAvailabilityBeforeBookingOutcome: 'CAO_SUCCESS',
    tokenizationOutcome: 'TO_SUCCESS',
    confirmationOutcome: 'CO_SUCCESS',
    mqEventOutcome: 'MEO_NO_EVENT',
};
