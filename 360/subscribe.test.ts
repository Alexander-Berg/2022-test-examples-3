import { Currency, IB2CTariff, IFeature, PaymentPeriod } from '../../../@types/common';

import { testSaga, expectSaga } from 'redux-saga-test-plan';

import { subscribeSaga } from './subscribe';
import { subscribeAction } from '../slices';
import { init as initTrustSaga } from '../../trust/sagas/init';
import { confirmPhone } from '../../beautifulEmail/sagas/confirmPhone';
import {
    getChosenEmail,
    getConnectedEmail,
    getDomainStatus,
    openLightTariffDialog,
    resetLightTariffDialog,
    returnToChooseTariff,
    continueWithLightTariff,
    DomainStatus
} from '../../beautifulEmail/slices';
import { isMobile } from '../../../slices';
import { select, call, take } from 'redux-saga/effects';
import { DOMAIN_FEATURE_CODE } from '../../../constants';

jest.mock('../../../../utils/metrika', () => ({
    countMetrika: jest.fn(),
}));

describe('Tariffs/Sagas', () => {
    describe('Subscribe saga', () => {
        const priceId = 'price-id';
        const getTariff = (features: IFeature[] = []): IB2CTariff => ({
            product_id: 'product-id',
            title: 'title',
            best_offer: false,
            features,
            prices: [{
                price_id: priceId,
                amount: 123,
                currency: Currency.RUB,
                period: PaymentPeriod.month
            }]
        });
        const someFeature = {
            description: 'my feature',
            code: 'feature1',
            enabled: true,
        };
        const domainFeature = {
            code: DOMAIN_FEATURE_CODE,
            description: 'Красивый адрес',
            enabled: true,
        };

        it('should spawn trust init saga', () => {
            const tariff = getTariff([someFeature, domainFeature]);

            testSaga(subscribeSaga, subscribeAction({ tariff, priceId }))
                .next()
                .select(getChosenEmail)
                .next(undefined)
                .select(getConnectedEmail)
                .next(undefined)
                .spawn(initTrustSaga, { tariff, priceId })
                .next()
                .isDone();
        });

        it('should open confirm if has chosen domain and tariff w/o domain', () => {
            const tariff = getTariff([someFeature]);

            return expectSaga(subscribeSaga, subscribeAction({ tariff, priceId }))
                .provide([
                    [select(getChosenEmail), 'a@b.ru'],
                    [select(getConnectedEmail), undefined]
                ])
                .put(openLightTariffDialog(tariff.product_id))
                .silentRun();
        });

        it('should open confirm if has connected domain and tariff w/o domain', () => {
            const tariff = getTariff([someFeature]);

            return expectSaga(subscribeSaga, subscribeAction({ tariff, priceId }))
                .provide([
                    [select(getChosenEmail), undefined],
                    [select(getConnectedEmail), 'a@b.ru']
                ])
                .put(openLightTariffDialog(tariff.product_id))
                .silentRun();
        });

        it('should not open confirm if has chosen domain and tariff with domain', () => {
            const tariff = getTariff([someFeature, domainFeature]);

            return expectSaga(subscribeSaga, subscribeAction({ tariff, priceId }))
                .provide([
                    [select(getChosenEmail), 'a@b.ru'],
                    [select(getConnectedEmail), undefined],
                    [select(getDomainStatus), DomainStatus.shouldFetch],
                    [select(isMobile), false],
                    [call(confirmPhone), ''],
                ])
                .not.put(openLightTariffDialog(tariff.product_id))
                .silentRun();
        });

        it('should not init trust if has chosen domain and tariff w/o domain and decide to return', () => {
            const tariff = getTariff([someFeature]);

            testSaga(subscribeSaga, subscribeAction({ tariff, priceId }))
                .next()
                .select(getChosenEmail)
                .next('a@b.ru')
                .select(getConnectedEmail)
                .next(undefined)
                .put(openLightTariffDialog(tariff.product_id))
                .next()
                .race({
                    returnToChoose: take(returnToChooseTariff.type),
                    continueWithLight: take(continueWithLightTariff.type),
                })
                .next({
                    returnToChoose: true,
                })
                .put(resetLightTariffDialog())
                .next()
                .isDone();
        });

        it('should init trust if has chosen domain and tariff w/o domain and decide to continue', () => {
            const tariff = getTariff([someFeature]);

            testSaga(subscribeSaga, subscribeAction({ tariff, priceId }))
                .next()
                .select(getChosenEmail)
                .next('a@b.ru')
                .select(getConnectedEmail)
                .next(undefined)
                .put(openLightTariffDialog(tariff.product_id))
                .next()
                .race({
                    returnToChoose: take(returnToChooseTariff.type),
                    continueWithLight: take(continueWithLightTariff.type),
                })
                .next({
                    continueWithLight: true,
                })
                .put(resetLightTariffDialog())
                .next()
                .spawn(initTrustSaga, { tariff, priceId })
                .next()
                .isDone();
        });
    });
});
