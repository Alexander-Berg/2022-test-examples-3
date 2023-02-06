import { hasActiveSubscription, hasActiveSubscriptionWithDomain } from '.';
import { DefaultRootState } from 'react-redux';
import { Currency, IB2CTariff, PaymentPeriod, IFeature, SubscriptionStatus } from '../../../@types/common';
import { DOMAIN_FEATURE_CODE } from '../../../constants';

describe('BeautifulEmail/Slices', () => {
    const someFeature: IFeature = {
        description: 'my feature',
        code: 'feature1',
        enabled: true
    };

    const domainFeatureEnabled: IFeature = {
        code: DOMAIN_FEATURE_CODE,
        description: 'Красивый адрес',
        enabled: true
    };
    const domainFeatureDisabled: IFeature = {
        ...domainFeatureEnabled,
        enabled: false
    };

    const tariffWithoutDomain: IB2CTariff = {
        product_id: 'product1',
        title: '111',
        best_offer: false,
        features: [someFeature, domainFeatureDisabled],
        prices: [{
            price_id: 'price1',
            period: PaymentPeriod.month,
            amount: 100,
            currency: Currency.RUB
        }]
    };

    const tariff1WithDomain: IB2CTariff = {
        product_id: 'product2',
        title: '222',
        best_offer: false,
        features: [someFeature, domainFeatureEnabled],
        prices: [{
            price_id: 'price2',
            period: PaymentPeriod.month,
            amount: 200,
            currency: Currency.RUB
        }]
    };

    const getState = (items: Array<{ product: IB2CTariff; status: SubscriptionStatus }>) => ({
        subscription: {
            items: items.map(({ product, status }) => ({
                subscription: {
                    product,
                    status
                }
            }))
        }
    }) as unknown as DefaultRootState;

    describe('hasActiveSubscriptionWithDomain', () => {
        it('should return false if no subscription (or it is fetching)', () => {
            expect(hasActiveSubscriptionWithDomain(getState([]))).toEqual(false);
        });

        it('should return false if active subscription has no domain', () => {
            expect(hasActiveSubscriptionWithDomain(getState([{
                product: tariffWithoutDomain,
                status: SubscriptionStatus.active
            }]))).toEqual(false);
        });

        it('should return false if subscription disabled', () => {
            expect(hasActiveSubscriptionWithDomain(getState([{
                product: tariff1WithDomain,
                status: SubscriptionStatus.disabled
            }]))).toEqual(false);
        });

        it('should return true if active subscription has domain', () => {
            expect(hasActiveSubscriptionWithDomain(getState([{
                product: tariff1WithDomain,
                status: SubscriptionStatus.active
            }]))).toEqual(true);
        });
    });

    describe('hasActiveSubscription', () => {
        it('should return false if user has no subscription', () => {
            expect(hasActiveSubscription(getState([]))).toEqual(false);
        });

        it('should return false if user subscription is disabled', () => {
            expect(hasActiveSubscription(getState([{
                product: tariffWithoutDomain,
                status: SubscriptionStatus.disabled
            }]))).toEqual(false);
        });

        it('should return true if user has subscription', () => {
            expect(hasActiveSubscription(getState([{
                product: tariffWithoutDomain,
                status: SubscriptionStatus.active
            }]))).toEqual(true);
        });
    });
});
