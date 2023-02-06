import moment from 'moment';

import getScrolledMoment from '../getScrolledMoment';

describe('getScrolledMoment', () => {
    it('Передан только minDate в пределах первых двух недель от начала месяца - ничего не скролим', () => {
        expect(
            getScrolledMoment(
                undefined,
                undefined,
                moment('2022-02-12T10:00:00.000Z').toDate(),
                moment('2022-02-01T10:00:00.000Z').toDate(),
            ),
        ).toBeUndefined();
    });

    it('Передан только minDate равный первому дню третьей недели от начала месяца - скролим на одну неделю ранее', () => {
        expect(
            getScrolledMoment(
                undefined,
                undefined,
                moment('2022-02-22T10:00:00.000Z').toDate(),
                moment('2022-02-01T10:00:00.000Z').toDate(),
            )?.toISOString(),
        ).toBe('2022-02-15T10:00:00.000Z');
    });

    it('Передан только minDate равный последнему дню месяца, должны проскролить к предыдущей неделе от minDate', () => {
        expect(
            getScrolledMoment(
                undefined,
                undefined,
                moment('2022-02-28T10:00:00.000Z').toDate(),
                moment('2022-02-01T10:00:00.000Z').toDate(),
            )?.toISOString(),
        ).toBe('2022-02-21T10:00:00.000Z');
    });

    it('Передан minDate конец месяца и startDate это начало следующего месяца, должны проскролить к последнему дню предыдущего месяца', () => {
        expect(
            getScrolledMoment(
                moment('2022-03-01T10:00:00.000Z').toDate(),
                undefined,
                moment('2022-02-28T10:00:00.000Z').toDate(),
                moment('2022-02-01T10:00:00.000Z').toDate(),
            )?.toISOString(),
        ).toBe('2022-02-28T10:00:00.000Z');
    });

    it('Передан minDate конец месяца и startDate начало следующего месяца, endDate конец следующего месяца, проскролим к концу предыдущего месяца', () => {
        expect(
            getScrolledMoment(
                moment('2022-03-01T10:00:00.000Z').toDate(),
                moment('2022-03-31T10:00:00.000Z').toDate(),
                moment('2022-02-28T10:00:00.000Z').toDate(),
                moment('2022-02-01T10:00:00.000Z').toDate(),
            )?.toISOString(),
        ).toBe('2022-02-28T10:00:00.000Z');
    });
});
