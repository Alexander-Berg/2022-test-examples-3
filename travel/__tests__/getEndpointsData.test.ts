import SegmentEventType from '../../../../interfaces/segment/SegmentEventType';
import ISegment from '../../../../interfaces/segment/ISegment';
import ITransfer from '../../../../interfaces/transfer/ITransfer';

import getEndpointsData from '../getEndpointsData';

import delayKeyset from '../../../../i18n/delay-status';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import segmentkeyset from '../../../../i18n/segment';

jest.mock('../../../../i18n/delay-status', () => jest.fn(value => value));
jest.mock('../../../../i18n/segment', () => jest.fn(value => value));

const delayFact = {
    type: SegmentEventType.fact,
    minutesFrom: 5,
    minutesTo: 5,
};
const possibleDelay = {
    type: SegmentEventType.possibleDelay,
    minutesFrom: 5,
    minutesTo: 20,
};

describe('getEndpointsData', () => {
    it('Нет события отправления - вернём null', () => {
        const segment = {arrivalEvent: possibleDelay} as ISegment;

        expect(getEndpointsData(segment)).toBeNull();
    });

    it('Нет события отправления у ушедшего сегмента - вернем объект с информацией, что сегмент ушел', () => {
        const segment = {isGone: true, departureEvent: null} as ISegment;

        expect(getEndpointsData(segment)).toEqual({
            isDelay: false,
            isGone: true,
            text: 'gone-title',
        });
    });

    it('Есть событие отправления - вернём соответствующий объект', () => {
        expect(
            getEndpointsData({
                departureEvent: possibleDelay,
            } as ISegment),
        ).toEqual({
            eventType: SegmentEventType.possibleDelay,
            isDelay: true,
            text: SegmentEventType.possibleDelay,
        });
    });

    it('Есть событие по отправлению, но задержка не > 1 минуты - вернём соответствующий объект', () => {
        const segment = {
            departureEvent: {
                type: SegmentEventType.fact,
                minutesFrom: 1,
                minutesTo: 1,
            },
        } as ISegment;

        expect(getEndpointsData(segment)).toEqual({
            eventType: SegmentEventType.fact,
            isDelay: false,
            text: 'ok',
        });
    });

    it('Есть событие по отправлению, есть задержка > 1 минуты - вернём соответствующий объект', () => {
        const segment = {
            departureEvent: {
                type: SegmentEventType.fact,
                minutesFrom: 2,
                minutesTo: 2,
            },
        } as ISegment;

        expect(getEndpointsData(segment)).toEqual({
            eventType: SegmentEventType.fact,
            isDelay: true,
            text: 'delay',
        });
    });

    it('Известно точное время опоздания', () => {
        getEndpointsData({
            departureEvent: delayFact,
        } as ISegment);

        expect(delayKeyset).toHaveBeenCalledWith('delay', {
            ...delayFact,
            isRange: false,
        });
    });

    it('Известен диапазон опоздания', () => {
        getEndpointsData({
            departureEvent: possibleDelay,
        } as ISegment);

        expect(delayKeyset).toHaveBeenCalledWith(
            SegmentEventType.possibleDelay,
            {
                ...possibleDelay,
                isRange: true,
            },
        );
    });

    it(`Если сегмент уже ушел, но при этом еще нет фактических данных об опозданиях, то в 
    возвращаемом объекте будет текст о том, что сегмент ушел, а признак опоздания будет false`, () => {
        expect(
            getEndpointsData({
                departureEvent: possibleDelay,
                isGone: true,
            } as ISegment),
        ).toEqual({
            isDelay: false,
            isGone: true,
            text: 'gone-title',
        });
    });

    it('Для пересадки вернет null', () => {
        expect(
            getEndpointsData({
                isTransfer: true,
            } as ITransfer),
        ).toBeNull();
    });

    it('Для ушедшей пересадки вернет объект с информацией, что пересадка ушла', () => {
        expect(
            getEndpointsData({
                isTransfer: true,
                isGone: true,
            } as ITransfer),
        ).toEqual({
            isDelay: false,
            isGone: true,
            text: 'gone-title',
        });
    });
});
