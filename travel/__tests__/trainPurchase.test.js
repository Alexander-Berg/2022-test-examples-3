jest.disableAutomock();

import getTrainPurchasePath from '../trainPurchasePath';

const language = 'ru';
const orderId = '123';

describe('getTrainPurchasePath', () => {
    it('no orderId and no action passed', () => {
        expect(getTrainPurchasePath(language)).toBe(
            '/ru/api/train-purchase/orders/',
        );
    });

    it('orderId passed, no action passed', () => {
        expect(getTrainPurchasePath(language, orderId)).toBe(
            '/ru/api/train-purchase/orders/123/',
        );
    });

    it('action passed, no orderId passed', () => {
        expect(getTrainPurchasePath(language, null, 'find')).toBe(
            '/ru/api/train-purchase/orders/find/',
        );
    });

    it('both orderId and action passed', () => {
        expect(getTrainPurchasePath(language, orderId, 'refund')).toBe(
            '/ru/api/train-purchase/orders/123/refund/',
        );
    });
});
