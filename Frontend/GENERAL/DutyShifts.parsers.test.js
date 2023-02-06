import {
    prepareShiftPatch,
    prepareReplacement,
} from './DutyShifts.parsers';

describe('DutyShifts.parser', () => {
    describe('prepareShiftPatch', () => {
        it('Should convert fields to backend format', () => {
            expect(prepareShiftPatch(1, null, null, { person: 'morty' }).person)
                .toBe('morty');
            expect(prepareShiftPatch(1, null, null, { is_approved: true }).is_approved)
                .toBe(true);
            expect(prepareShiftPatch(1, null, null, { replaces: [] }).replaces)
                .toBeInstanceOf(Array);
        });

        it('Should keep absent fields undefined', () => {
            expect(prepareShiftPatch(1, null, null, {}).id).toBeUndefined();
            expect(prepareShiftPatch(1, null, null, {}).is_approved).toBeUndefined();
            expect(prepareShiftPatch(1, null, null, {}).replaces).toBeUndefined();
        });
    });

    describe('prepareReplacement', () => {
        it('Should add shift id to the data', () => {
            expect(prepareReplacement(42, null, null, {}).replace_for)
                .toBe(42);
        });

        it('Should convert fields to backend format', () => {
            expect(prepareReplacement(1, null, null, { id: 42 }).id)
                .toBe(42);
            expect(prepareReplacement(1, null, null, { person: { login: 'rick' } }).person)
                .toBe('rick');
            expect(prepareReplacement(1, null, null, { startDate: new Date(Date.UTC(2000, 0, 1)) }).start_datetime)
                .toBe('2000-01-01T03:00:00.000+03:00');
            expect(prepareReplacement(1, null, null, { endDate: new Date(Date.UTC(2000, 0, 2)) }).end_datetime)
                .toBe('2000-01-02T03:00:00.000+03:00');
        });

        it('Should not exceed the parent shift boundaries', () => {
            const actual = prepareReplacement(
                1,
                '2000-01-04T10:00:00+03:00',
                '2000-01-05T10:00:00+03:00',
                {
                    startDate: new Date(Date.UTC(1999, 0, 1)),
                    endDate: new Date(Date.UTC(3000, 0, 10)),
                },
            );

            expect(actual.start_datetime).toBe('2000-01-04T10:00:00.000+03:00');
            expect(actual.end_datetime).toBe('2000-01-05T10:00:00.000+03:00');
        });
    });
});
