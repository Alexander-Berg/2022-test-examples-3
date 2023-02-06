import {ITrainsTariffApiSegment} from 'server/api/TrainsApi/types/ITrainsGetTariffsApi/models';

import highSpeedTrain from '../../highSpeedTrain';

describe('highSpeedTrain.apply', () => {
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

    const segmentWithNullThread = {
        thread: null,
    } as ITrainsTariffApiSegment;

    it('is highspeed segment on empty array', () => {
        const value: string[] = [];
        const result = highSpeedTrain.apply(value, highSpeedSegment);

        expect(result).toBe(false);
    });

    it('is slowpoke segment on empty array', () => {
        const value: string[] = [];
        const result = highSpeedTrain.apply(value, slowpokeSegment);

        expect(result).toBe(false);
    });

    it('is highspeed segment', () => {
        const value = ['1', '2'];
        const result = highSpeedTrain.apply(value, highSpeedSegment);

        expect(result).toBe(true);
    });

    it('is slowpoke segment', () => {
        const value = ['1', '2'];
        const result = highSpeedTrain.apply(value, slowpokeSegment);

        expect(result).toBe(false);
    });

    it('should return false on segment with null thread field', () => {
        const value = ['1', '2'];
        const result = highSpeedTrain.apply(value, segmentWithNullThread);

        expect(result).toBe(false);
    });
});
