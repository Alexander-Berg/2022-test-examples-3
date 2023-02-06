import {YBUS} from '../../../tariffSources';

import mockCurrencyCode from '../../../../../interfaces/CurrencyCode';
import {OrderUrlOwner} from '../../../tariffClasses';
import ISegmentTariffs from '../../../../../interfaces/segment/ISegmentTariffs';
import ISegmentTariffClassFromBackend from '../../../../../interfaces/segment/ISegmentTariffClassFromBackend';
import ITransferSegmentFromApi from '../../../../../interfaces/transfer/ITransferSegmentFromApi';

import patchTariffs from '../../patchTariffs';
import getTrainUrl from '../../../../url/getTrainUrl';
import {
    busSegment,
    busTariffs,
    meta,
    updatedBusTariffs,
    trainSegment,
    trainTariffsFromUfs,
    trainTariffsFromTrains,
    trainTariffsFromUnknown,
    trainTariffsWithTrainsLink,
    trainTariffsWithUfsLink,
    trainTariffsWithUnknownLink,
} from './patchTariffs.const';

jest.mock('../../../../url/ufs-buy', () =>
    jest.fn(
        () =>
            'https://www.ufs-online.ru/kupit-zhd-bilety/8000800/2000200?date=20.02.2018&domain=yandex.ufs-online.ru&trainNumber=2',
    ),
);
jest.mock('../../../../currencies/convertPrice', () =>
    jest.fn(price => ({
        value: price.value * 65,
        currency: mockCurrencyCode.rub,
    })),
);
jest.mock('../../../../url/getTrainUrl', () =>
    jest.fn(() => 'absoluteUrlToTrains'),
);

describe('patchTariffs function', () => {
    it('Должна вернуть обновленные тарифы', () => {
        const result = patchTariffs(busSegment, busTariffs, meta);

        expect(result).toEqual(updatedBusTariffs);
    });

    it('Должна назначить orderUrl если провайдером покупки являются поезда', () => {
        const result = patchTariffs(trainSegment, trainTariffsFromTrains, meta);

        expect(result).toEqual(trainTariffsWithTrainsLink);
    });

    it('Должна назначить orderUrl если провайдером покупки является УФС', () => {
        const result = patchTariffs(trainSegment, trainTariffsFromUfs, meta);

        expect(result).toEqual(trainTariffsWithUfsLink);
    });

    it('Должна присвоить пустую строку в orderUrl, если провайдер покупки неизвестен', () => {
        const result = patchTariffs(
            trainSegment,
            trainTariffsFromUnknown,
            meta,
        );

        expect(result).toEqual(trainTariffsWithUnknownLink);
    });

    it('Тач, автобусные сегменты - для сегментов с динамической продажей (Я.Автобусы)', () => {
        const touchMeta = {
            ...meta,
            isTouch: true,
        };
        const result = patchTariffs(
            {
                ...busSegment,
                source: YBUS,
            },
            busTariffs,
            touchMeta,
        );

        expect(result.classes.bus.orderUrl).toBe(
            busTariffs.classes.bus.orderUrl,
        );
    });

    it(`Для сегмента пересадки поездов, в которой указана только абсолютная ссылка на покупку, 
    вернет сегмент с ценами в национальной валюте и не будет менять ссылку на покупку`, () => {
        const tariffs = {
            classes: {
                suite: {
                    price: {
                        value: 10,
                        currency: mockCurrencyCode.usd,
                    },
                    orderUrl: 'absoluteLink',
                    trainOrderUrlOwner: OrderUrlOwner.trains,
                },
            },
        } as ISegmentTariffs<ISegmentTariffClassFromBackend>;

        const segment = {
            ...trainSegment,
            isTransferSegment: true,
        } as unknown as ITransferSegmentFromApi;

        const result = patchTariffs(segment, tariffs, meta);

        expect(result).toEqual({
            classes: {
                suite: {
                    price: {
                        value: 10,
                        currency: mockCurrencyCode.usd,
                    },
                    nationalPrice: {
                        value: 650,
                        currency: mockCurrencyCode.rub,
                    },
                    orderUrl: 'absoluteLink',
                    trainOrderUrlOwner: OrderUrlOwner.trains,
                },
            },
        });
    });

    it(`Для сегмента пересадки поездов, в которой указана trainOrderUrl (относительная ссылка на покупку), 
    вернет сегмент с ценами в нациоанльной валюте и сгенерирует абсолютную ссылку на покупку (orderUrl)`, () => {
        const tariffs = {
            classes: {
                suite: {
                    price: {
                        value: 10,
                        currency: mockCurrencyCode.usd,
                    },
                    orderUrl: 'absoluteLink',
                    trainOrderUrl: 'relativeLink',
                    trainOrderUrlOwner: OrderUrlOwner.trains,
                },
            },
        } as ISegmentTariffs<ISegmentTariffClassFromBackend>;

        const segment = {
            ...trainSegment,
            isTransferSegment: true,
        } as unknown as ITransferSegmentFromApi;

        const result = patchTariffs(segment, tariffs, meta);

        expect(getTrainUrl).toBeCalled();
        expect(result).toEqual({
            classes: {
                suite: {
                    price: {
                        value: 10,
                        currency: mockCurrencyCode.usd,
                    },
                    nationalPrice: {
                        value: 650,
                        currency: mockCurrencyCode.rub,
                    },
                    orderUrl: 'absoluteUrlToTrains',
                    trainOrderUrl: 'relativeLink',
                    trainOrderUrlOwner: OrderUrlOwner.trains,
                },
            },
        });
    });
});
