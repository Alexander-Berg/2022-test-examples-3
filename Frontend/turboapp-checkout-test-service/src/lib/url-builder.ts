export enum UrlTemplate {
    MainPage = '/',
    TestMerchant = '/test-merchant',
    EventManager = '/event-manager',
    TestPaymentForm = '/test-payment-form',
}

export const getMainPageUrl = () => UrlTemplate.MainPage;
export const getTestMerchantUrl = () => UrlTemplate.TestMerchant;
export const getEventManagerUrl = () => UrlTemplate.EventManager;
export const getTestPaymentFormUrl = () => UrlTemplate.TestPaymentForm;
export const getDefaultTestPaymentFormUrl = () => `${window.location.origin}${getTestPaymentFormUrl()}`;
