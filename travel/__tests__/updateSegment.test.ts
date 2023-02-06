import {TransportType} from '../../transportType';
import ISearchMeta from '../../../interfaces/state/search/ISearchMeta';
import ISegment from '../../../interfaces/segment/ISegment';
import DateMoment from '../../../interfaces/date/DateMoment';
import StationType from '../../../interfaces/state/station/StationType';
import StationSubtype from '../../../interfaces/state/station/StationSubtype';

import patchTariffs from '../tariffs/patchTariffs';
import updateSegment from '../updateSegment';

jest.mock('../../order/shouldUseLinkToTrains', () => jest.fn(() => false));
jest.mock('../tariffs/patchTariffs', () => jest.fn(segment => segment.tariffs));
jest.mock('../patchSegments', () => jest.fn(({segments}) => segments));
jest.mock('../isUpdateCheaper', () =>
    jest.fn(
        (a, b) =>
            a.tariffs?.classes.bus.price.value >
            b.tariffs?.classes.bus.price.value,
    ),
);

const meta = {} as ISearchMeta;

type Station = {
    id: number;

    pageType?: StationType;
    mainSubtype?: StationSubtype;
};

interface IGetSegment {
    transportType?: TransportType;
    priceValue?: number;
    number?: string;
    originalNumber?: string;
    hasTrainTariffs?: boolean;
    codeshares?: {name: string; number: string}[];
    company?: {name: string};
    url?: string;
    stationFrom?: Station;
    stationTo?: Station;
    departure?: DateMoment;
}

function getSegment({
    transportType = TransportType.bus,
    priceValue,
    number,
    originalNumber,
    hasTrainTariffs,
    codeshares,
    company,
    url,
    stationFrom = {id: 1},
    stationTo = {id: 2},
    departure = '2020-03-11T15:40:00+03:00' as DateMoment,
}: IGetSegment): ISegment {
    return {
        transport: {code: transportType},
        originalNumber,
        number,
        hasTrainTariffs,
        codeshares:
            codeshares &&
            codeshares.map(codeshare => ({
                company: {name: codeshare.name},
                number: codeshare.number,
            })),
        company: {name: company?.name},
        url,
        stationFrom,
        stationTo,
        departure,
        ...(priceValue
            ? {
                  tariffs: {
                      classes: {
                          bus: {
                              price: {
                                  value: priceValue,
                                  currency: 'RUR',
                              },
                          },
                      },
                      orderUrl: 'http://ya.ru',
                  },
              }
            : null),
    } as unknown as ISegment;
}

const baseSegment = getSegment({
    transportType: TransportType.train,
    number: '55БФ',
    priceValue: 1000,
});

const segmentWithMoreExpensiveTariffs = getSegment({
    transportType: TransportType.train,
    originalNumber: '12T',
    number: '12T',
    priceValue: 2000,
});

const segmentWithCheaperTariffs = getSegment({
    transportType: TransportType.train,
    originalNumber: '13T',
    number: '13T',
    priceValue: 900,
});

describe('updateSegment', () => {
    it('Вернёт базовый сегмент, если он содержит наименьшую цену', () => {
        const result = updateSegment(
            baseSegment,
            [segmentWithMoreExpensiveTariffs],
            meta,
        );

        expect(result).toEqual(baseSegment);
    });

    it(`Заменяем базовый сегмент, если в сегменте от партнёра отличается время отправления.
        Считаем информацию от партнёра более актуальной.`, () => {
        const segmentWithDifferentDeparture = {
            ...segmentWithMoreExpensiveTariffs,
            departure: '2016-01-01T15:30:00' as DateMoment,
        };
        const result = updateSegment(
            {
                ...baseSegment,
                departure: '2016-01-01T16:30:00' as DateMoment,
            },
            [segmentWithDifferentDeparture],
            meta,
        );

        expect(result).toBe(segmentWithDifferentDeparture);
    });

    it('Вернёт расширенный сегмент', () => {
        const result = updateSegment(
            baseSegment,
            [segmentWithCheaperTariffs],
            meta,
        );

        expect(result).toEqual({
            ...baseSegment,
            ...segmentWithCheaperTariffs,
        });
    });

    it('Если сегмент является мета-сегментом - не заменяем у него номер', () => {
        const result = updateSegment(
            {
                ...baseSegment,
                isMetaSegment: true,
            },
            [segmentWithCheaperTariffs],
            meta,
        );

        expect(result.number).toBe('55БФ');
    });

    it('Возвращаем оригинальный номер для электричек с возможностью продажи', () => {
        const originSegment = getSegment({
            transportType: TransportType.suburban,
            hasTrainTariffs: true,
            number: '7081/7082',
        });
        const mergeSegment = getSegment({
            transportType: TransportType.suburban,
            number: '7081И',
        });

        expect(
            updateSegment(originSegment, [mergeSegment], meta).number,
        ).toEqual('7081/7082');
    });

    it('Возвращаем оригинальный номер если сегмент с ценой не содержит номера', () => {
        const originSegment = getSegment({
            transportType: TransportType.train,
            number: '55Ч',
        });

        const trainSegment = getSegment({transportType: TransportType.train});

        expect(
            updateSegment(originSegment, [trainSegment], meta).number,
        ).toEqual('55Ч');
    });

    it('Для всех остальных случаев возвращаем номер из сегмента с ценой', () => {
        const originSegment = getSegment({
            transportType: TransportType.train,
            number: '55Ч',
        });
        const mergeSegment = getSegment({
            transportType: TransportType.train,
            number: '60Я',
            priceValue: 1000,
        });

        expect(
            updateSegment(originSegment, [mergeSegment], meta).number,
        ).toEqual('60Я');
    });

    it('Кодшерные рейсы', () => {
        const planeSegment = getSegment({
            transportType: TransportType.plane,
            originalNumber: 'o1',
            number: '1',
            company: {name: 'name1'},
            codeshares: [
                {
                    number: '2',
                    name: 'name2',
                },
            ],
        });

        const planeTariff = getSegment({
            transportType: TransportType.plane,
            originalNumber: 'o1',
            number: '1',
            priceValue: 1000,
            company: {name: 'name1'},
            url: 'url1',
        });

        const codeshareSegment = getSegment({
            transportType: TransportType.plane,
            originalNumber: 'o2',
            number: '2',
            priceValue: 500,
            url: 'url2',
            company: {name: 'name2'},
        });

        const result = updateSegment(
            planeSegment,
            [codeshareSegment, planeTariff],
            meta,
        );

        expect(result.number).toBe(planeSegment.number);
        expect(result.originalNumber).toBe(planeSegment.originalNumber);
        // Найдем url в тарифах
        expect(result.url).toBe(planeTariff.url);
        expect(result.company).toBe(planeSegment.company);
        // Должна быт информация о кодшерной компании в тарифе
        expect(patchTariffs as jest.Mock).toBeCalledWith(
            codeshareSegment,
            codeshareSegment.tariffs,
            meta,
            planeSegment.codeshares?.[0].company,
            planeSegment.codeshares?.[0].number,
        );
    });

    it('Сохраняем данные о станции от базового сегмента, если id станции базового сегмента и тарифа совпадают', () => {
        const segment = getSegment({
            transportType: TransportType.train,
            stationFrom: {
                id: 1,
                pageType: StationType.railroad,
                mainSubtype: StationSubtype.train,
            },
            stationTo: {
                id: 2,
                pageType: StationType.railroad,
                mainSubtype: StationSubtype.train,
            },
        });

        const trainTariff = getSegment({
            transportType: segment.transport.code,
            priceValue: 1000,
            stationFrom: {
                id: segment.stationFrom.id,
            },
            stationTo: {
                id: segment.stationTo.id,
            },
        });

        const result = updateSegment(segment, [trainTariff], meta);

        // Удостоверимся, что тариф был добавлен в сегмент
        expect(segment.tariffs).toBeUndefined();
        expect(typeof result.tariffs).toBe('object');

        expect(result.stationFrom).toMatchObject(segment.stationFrom);
        expect(result.stationTo).toMatchObject(segment.stationTo);
    });

    it('Берем данные о станции от тарифа, если id станции базового сегмента и тарифа не совпадают', () => {
        const segment = getSegment({
            transportType: TransportType.train,
            stationFrom: {
                id: 1,
                pageType: StationType.railroad,
                mainSubtype: StationSubtype.train,
            },
            stationTo: {
                id: 2,
                pageType: StationType.railroad,
                mainSubtype: StationSubtype.train,
            },
        });

        const suburbanStationData = {
            pageType: StationType.railroad,
            mainSubtype: StationSubtype.suburban,
        };

        const trainTariff = getSegment({
            transportType: segment.transport.code,
            priceValue: 1000,
            stationFrom: {
                id: segment.stationFrom.id + 1,
                ...suburbanStationData,
            },
            stationTo: {
                id: segment.stationTo.id + 1,
                ...suburbanStationData,
            },
        });

        const result = updateSegment(segment, [trainTariff], meta);

        // Удостоверимся, что тариф был добавлен в сегмент
        expect(segment.tariffs).toBeUndefined();
        expect(typeof result.tariffs).toBe('object');

        expect(result.stationFrom).toMatchObject(trainTariff.stationFrom);
        expect(result.stationTo).toMatchObject(trainTariff.stationTo);
    });
});
