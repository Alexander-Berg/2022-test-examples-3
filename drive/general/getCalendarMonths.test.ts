import { getCalendarMonths } from 'shared/helpers/getCalendarMonths/getCalendarMonths';

describe('getCalendarMonths', function () {
    beforeEach(function () {
        jest.useFakeTimers().setSystemTime(new Date('2022-01-12').getTime());
    });

    afterEach(function () {
        jest.useRealTimers();
    });

    it('works with empty params', function () {
        expect(getCalendarMonths(null)).toMatchInlineSnapshot(`
            Array [
              Object {
                "date": 2022-01-01T00:00:00.000Z,
                "month": 0,
                "year": 2022,
              },
            ]
        `);
    });

    it('works width positive step', function () {
        expect(getCalendarMonths(new Date('2000-09-12'), 1)).toMatchInlineSnapshot(`
            Array [
              Object {
                "date": 2000-09-01T00:00:00.000Z,
                "month": 8,
                "year": 2000,
              },
            ]
        `);

        expect(getCalendarMonths(new Date('2000-09-12'), 2)).toMatchInlineSnapshot(`
            Array [
              Object {
                "date": 2000-09-01T00:00:00.000Z,
                "month": 8,
                "year": 2000,
              },
              Object {
                "date": 2000-10-01T00:00:00.000Z,
                "month": 9,
                "year": 2000,
              },
            ]
        `);
    });

    it('works width negative step', function () {
        expect(getCalendarMonths(new Date('2000-09-12'), -1)).toMatchInlineSnapshot(`
            Array [
              Object {
                "date": 2000-09-01T00:00:00.000Z,
                "month": 8,
                "year": 2000,
              },
            ]
        `);

        // eslint-disable-next-line @typescript-eslint/no-magic-numbers
        expect(getCalendarMonths(new Date('2000-09-12'), -2)).toMatchInlineSnapshot(`
            Array [
              Object {
                "date": 2000-09-01T00:00:00.000Z,
                "month": 8,
                "year": 2000,
              },
              Object {
                "date": 2000-08-01T00:00:00.000Z,
                "month": 7,
                "year": 2000,
              },
            ]
        `);
    });
});
