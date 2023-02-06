const getTariffs = require('./get-tariffs');

describe('model:ps-billing → get-tariffs', () => {
    let coreMock;
    let serviceFn;
    let psBillingFn;

    beforeEach(() => {
        serviceFn = jest.fn();
        psBillingFn = jest.fn();
        coreMock = {
            service: serviceFn,
            req: {
                lang: 'uk',
                query: {}
            }
        };

        serviceFn.mockReturnValue(psBillingFn);
    });

    describe('#', () => {
        test('should call the ps-billing service', () => {
            psBillingFn.mockResolvedValueOnce({});
            getTariffs({ isUsdPaymentExp: false }, coreMock);

            expect(serviceFn).toHaveBeenCalledWith('psBilling');
            expect(psBillingFn).toHaveBeenCalledWith(
                '/v3/productsets/mail_pro_b2c/products',
                {
                    lang: 'uk',
                    currency: 'RUB',
                    skip_disabled_features: false,
                    promo_activation: true,
                    payload_type: 'web_tuning',
                    payload_version: 0
                }
            );
        });
    });
});
