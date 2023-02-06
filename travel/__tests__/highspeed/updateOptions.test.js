import {FilterTransportType} from '../../../transportType';

import highspeed from '../../highSpeedTrain';

describe('highspeed.updateOptions', () => {
    const highSpeedSegment = {
        transport: {
            code: FilterTransportType.train,
        },
        thread: {
            deluxeTrain: {
                id: '1',
                title: 'Flash',
                isHighSpeed: true,
            },
        },
    };
    const slowpokeSegment = {
        transport: {
            code: FilterTransportType.train,
        },
        thread: {
            deluxeTrain: {
                id: '2',
                title: 'Slowpoke',
                isHighSpeed: false,
            },
        },
    };
    const bus = {
        transport: {
            code: FilterTransportType.bus,
        },
        thread: {},
    };

    const highspeedOptions = [
        {
            value: '1',
            text: 'Flash',
        },
    ];

    it('Вернёт опции расширенные информацией о скоростном поезде', () => {
        const options = [];
        const result = highspeed.updateOptions(options, highSpeedSegment);

        expect(result).toEqual(highspeedOptions);
    });

    it('Если данные о скоростном поезде уже есть - вернём опции без изменений', () => {
        const result = highspeed.updateOptions(
            highspeedOptions,
            highSpeedSegment,
        );

        expect(result).toEqual(highspeedOptions);
    });

    it('Если электричка не скоростная - вернём опции без изменений', () => {
        const result = highspeed.updateOptions(
            highspeedOptions,
            slowpokeSegment,
        );

        expect(result).toEqual(highspeedOptions);
    });

    it('Если тип транспорта не электричка - вернём опции без изменений', () => {
        const options = [];
        const result = highspeed.updateOptions(options, bus);

        expect(result).toEqual([]);
    });
});
