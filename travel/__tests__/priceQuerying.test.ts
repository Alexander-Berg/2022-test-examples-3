import upperFirst from 'lodash/upperFirst';

import {TransportType} from '../../transportType';

import ISegment from '../../../interfaces/segment/ISegment';
import ITransfer from '../../../interfaces/transfer/ITransfer';
import ITransferSegment from '../../../interfaces/transfer/ITransferSegment';

import isPriceQuerying from '../priceQuerying';

const busSegment = {transport: {code: TransportType.bus}};
const trainSegment = {transport: {code: TransportType.train}};
const planeSegment = {transport: {code: TransportType.plane}};
const suburbanSegment = {transport: {code: TransportType.suburban}};

describe('isPriceQuerying', () => {
    it('Если опрос цен не запущен - вернёт false', () => {
        const querying = {
            plane: false,
            train: false,
        };

        expect(isPriceQuerying(querying, trainSegment as ISegment)).toBe(false);
    });

    it(
        'Если запущен только опрос жд цен, но сегмент не является ' +
            'поездом или электричкой с возможностью продажи - вернёт false',
        () => {
            const querying = {
                plane: false,
                train: true,
            };

            expect(isPriceQuerying(querying, busSegment as ISegment)).toBe(
                false,
            );
        },
    );

    it('Если запущен опрос жд цен и сегмент является поездом - вернёт true', () => {
        const querying = {
            plane: false,
            train: true,
        };

        expect(isPriceQuerying(querying, trainSegment as ISegment)).toBe(true);
    });

    it('Если запущен опрос жд цен и сегмент является электричкой с возможностью продажи - вернёт true', () => {
        const querying = {
            plane: false,
            train: true,
        };

        expect(
            isPriceQuerying(querying, {
                ...suburbanSegment,
                hasTrainTariffs: true,
            } as ISegment),
        ).toBe(true);
    });

    it('Для сегмента с интервальным рейсом - вернёт false', () => {
        const querying = {
            plane: true,
            train: true,
        };

        expect(
            isPriceQuerying(querying, {
                ...trainSegment,
                isInterval: true,
            } as ISegment),
        ).toBe(false);
    });

    it('Если опрос динамических цен запущен и сегмент доступен для опроса - вернём true', () => {
        const querying = {
            plane: true,
            train: false,
        };

        expect(isPriceQuerying(querying, trainSegment as ISegment)).toBe(true);
    });

    it(`Если опрос поездатых цен продолжается, но у электрички нет признака доступности
    жд-тарифов, вернет false`, () => {
        const querying = {train: true};
        const segment = {
            ...suburbanSegment,
            hasTrainTariffs: false,
        } as ISegment;

        expect(isPriceQuerying(querying, segment)).toBe(false);
    });

    it(`Если полинг поездатых цен продолжается, и у элеткрички есть признак доступности
    жд-тарифов, вернет true`, () => {
        const querying = {train: true};
        const segment = {
            ...suburbanSegment,
            hasTrainTariffs: true,
        } as ISegment;

        expect(isPriceQuerying(querying, segment)).toBe(true);
    });

    it('Если полинг самолетных цен продолжается, то вернет true для самолетных сегментов', () => {
        const querying = {plane: true};

        expect(isPriceQuerying(querying, planeSegment as ISegment)).toBe(true);
    });

    it(`Для сегмента автобусной/электричечной пересадки вернет false,
    несмотря на наличие активного полинга цен`, () => {
        const querying = {transferAll: true};
        const busTransferSegment = {
            ...busSegment,
            isTransferSegment: true,
        } as ITransferSegment;
        const suburbanTransferSegment = {
            ...suburbanSegment,
            isTransferSegment: true,
        } as ITransferSegment;

        expect(isPriceQuerying(querying, busTransferSegment)).toBe(false);
        expect(isPriceQuerying(querying, suburbanTransferSegment)).toBe(false);
    });

    it(`Для сегмента самолетной пересадки вернет true в случае, если происходит 
    полинг цен пересадок всеми видами транспорта и самолетами`, () => {
        const planeTransferSegment = {
            ...planeSegment,
            isTransferSegment: true,
        } as ITransferSegment;

        expect(isPriceQuerying({transferAll: true}, planeTransferSegment)).toBe(
            true,
        );
        expect(
            isPriceQuerying({transferTrain: true}, planeTransferSegment),
        ).toBe(false);
        expect(
            isPriceQuerying({transferPlane: true}, planeTransferSegment),
        ).toBe(true);
    });

    it(`Для сегмента поездатой пересадки вернет true в случае, если происходит 
    полинг цен пересадок для поездов или всеми видами транспорта`, () => {
        const trainTransferSegment = {
            ...trainSegment,
            isTransferSegment: true,
        } as ITransferSegment;

        expect(isPriceQuerying({transferAll: true}, trainTransferSegment)).toBe(
            true,
        );
        expect(
            isPriceQuerying({transferTrain: true}, trainTransferSegment),
        ).toBe(true);
    });

    it(`Для самолетной пересадки вернет true, если все ее сегменты самолетные и 
    происходит полинг цен пересадок всеми видами транспорта или самолетами`, () => {
        const planeTransferSegment = {
            ...planeSegment,
            isTransferSegment: true,
        } as ITransferSegment;
        const busTransferSegment = {
            ...busSegment,
            isTransferSegment: true,
        } as ITransferSegment;
        const transferWithPlane = {
            isTransfer: true,
            transport: {code: TransportType.plane},
            segments: [planeTransferSegment, busTransferSegment],
        } as ITransfer;
        const transferInterline = {
            isTransfer: true,
            transport: {code: TransportType.plane},
            segments: [planeTransferSegment, planeTransferSegment],
        } as ITransfer;

        expect(isPriceQuerying({transferAll: true}, transferWithPlane)).toBe(
            false,
        );
        expect(isPriceQuerying({transferPlane: true}, transferWithPlane)).toBe(
            false,
        );
        expect(isPriceQuerying({transferBus: true}, transferWithPlane)).toBe(
            false,
        );

        expect(isPriceQuerying({transferAll: true}, transferInterline)).toBe(
            true,
        );
        expect(isPriceQuerying({transferPlane: true}, transferInterline)).toBe(
            true,
        );
        expect(isPriceQuerying({transferBus: true}, transferInterline)).toBe(
            false,
        );
    });

    Object.values(TransportType)
        .filter(transportType => transportType !== TransportType.plane)
        .forEach(transportType => {
            it(`Для пересадки типа транспорта ${transportType} вернет false вне зависимости
            от признаков полинга цен пересадок`, () => {
                const transfer = {
                    isTransfer: true,
                    transport: {code: transportType},
                    segments: [
                        {
                            isTransferSegment: true,
                            transport: {code: transportType},
                        },
                        {
                            isTransferSegment: true,
                            transport: {code: transportType},
                        },
                    ],
                } as ITransfer;

                expect(isPriceQuerying({transferAll: true}, transfer)).toBe(
                    false,
                );
                expect(
                    isPriceQuerying(
                        {[`transfer${upperFirst(transportType)}`]: true},
                        transfer,
                    ),
                ).toBe(false);
            });
        });
});
