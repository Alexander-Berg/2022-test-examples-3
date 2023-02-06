import ISegmentTariffClassFromApi from '../../../interfaces/segment/ISegmentTariffClassFromApi';
import CurrencyCode from '../../../interfaces/CurrencyCode';
import ISegmentFromApi from '../../../interfaces/segment/ISegmentFromApi';
import {TransportType} from '../../transportType';

import getBestSegment from '../getBestSegment';

interface IGetSegment {
    transportType?: TransportType;
    tariffClassName?: string;
    price?: number;
    orderUrl?: string;
}

function getSegment({
    transportType = TransportType.bus,
    tariffClassName = 'bus',
    price,
    orderUrl,
}: IGetSegment): ISegmentFromApi {
    const tariff = {} as ISegmentTariffClassFromApi;

    if (price) {
        tariff.price = {
            value: price,
            currency: CurrencyCode.rub,
        };
    }

    if (orderUrl) {
        tariff.orderUrl = orderUrl;
    }

    return {
        transport: {
            code: transportType,
        },
        tariffs: Object.keys(tariff).length
            ? {
                  classes: {
                      [tariffClassName]: tariff,
                  },
              }
            : undefined,
    } as ISegmentFromApi;
}

describe('getBestSegment', () => {
    it('Вернёт базовый сегмент как самый лучший, потому что у него цена меньше', () => {
        const baseSegment = getSegment({price: 100, orderUrl: 'url'});
        const updateSegment = getSegment({price: 200, orderUrl: 'url'});

        const result = getBestSegment(baseSegment, [updateSegment]);

        expect(result).toBe(baseSegment);
    });

    it('Вернёт сегмент с тарифами', () => {
        const updateSegment = getSegment({price: 200});

        const result = getBestSegment({} as ISegmentFromApi, [updateSegment]);

        expect(result).toBe(updateSegment);
    });

    it('Вернёт сегмент со ссылкой на покупку', () => {
        const baseSegment = getSegment({price: 100});
        const updateSegment = getSegment({price: 200, orderUrl: 'url'});

        const result = getBestSegment(baseSegment, [updateSegment]);

        expect(result).toBe(updateSegment);
    });

    it('Вернёт сегмент с наиболее низкой ценой', () => {
        const baseSegment = getSegment({price: 1000, orderUrl: 'url'});
        const cheaperSegment = getSegment({price: 900, orderUrl: 'url'});
        const expensiveSegment = getSegment({price: 2000, orderUrl: 'url'});

        const result = getBestSegment(baseSegment, [
            cheaperSegment,
            expensiveSegment,
        ]);

        expect(result).toBe(cheaperSegment);
    });

    it('Для электрички с поездатыми ценами должен вернуться сегмент с поездатыми ценами', () => {
        const suburbanSegment = getSegment({
            transportType: TransportType.suburban,
            price: 200,
        });
        const trainSegment = getSegment({
            transportType: TransportType.train,
            price: 300,
            orderUrl: 'url',
        });

        const result = getBestSegment(suburbanSegment, [trainSegment]);

        expect(result).toBe(trainSegment);
    });
});
