import {ITrainsSimpleFilterOption} from 'types/trains/search/filters/ITrainsFilters';
import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import highSpeedTrain from '../../highSpeedTrain';

describe('highSpeedTrain.updateOptions', () => {
    // @ts-ignore
    const highSpeedSegment = {
        thread: {
            deluxeTrain: {
                id: '1',
                title: 'Flash',
                isHighSpeed: true,
            },
        },
    } as ITrainsTariffApiSegment;

    // @ts-ignore
    const slowpokeSegment = {
        thread: {
            deluxeTrain: {
                id: '2',
                title: 'Slowpoke',
                isHighSpeed: false,
            },
        },
    } as ITrainsTariffApiSegment;

    const highspeedOptions = [
        {
            value: '1',
            text: 'Flash',
        },
    ];

    it('Вернёт опции расширенные информацией о скоростном поезде', () => {
        const options: ITrainsSimpleFilterOption[] = [];
        const result = highSpeedTrain.updateOptions(options, highSpeedSegment);

        expect(result).toEqual(highspeedOptions);
    });

    it('Если данные о скоростном поезде уже есть - вернём опции без изменений', () => {
        const result = highSpeedTrain.updateOptions(
            highspeedOptions,
            highSpeedSegment,
        );

        expect(result).toEqual(highspeedOptions);
    });

    it('Если электричка не скоростная - вернём опции без изменений', () => {
        const result = highSpeedTrain.updateOptions(
            highspeedOptions,
            slowpokeSegment,
        );

        expect(result).toEqual(highspeedOptions);
    });
});
