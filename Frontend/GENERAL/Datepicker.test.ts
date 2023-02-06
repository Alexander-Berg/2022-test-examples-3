import { parseDate, formatDate } from './Datepicker';

describe('Datepicker', () => {
    describe('parseDate() & formatDate()', () => {
        it('Should correctly serialize and deserialize', () => {
            expect(formatDate(parseDate('1995-05-13'))).toEqual('1995-05-13');
        });
    });

    describe('parseDate()', () => {
        it('Should generate date objects', () => {
            expect(parseDate('1995-05-13')).toBeInstanceOf(Date);
        });

        it('Should generate same date objects', () => {
            expect(parseDate('1995-05-13')).toEqual(parseDate('1995-05-13'));
        });

        it('Should return undefined with empty string arg', () => {
            expect(parseDate('')).toEqual(undefined);
        });

        it('Should return undefined with "undefined" arg', () => {
            expect(parseDate(undefined)).toEqual(undefined);
        });

        it('Should return undefined with "null" arg', () => {
            expect(parseDate(null)).toEqual(undefined);
        });
    });

    describe('formatDate()', () => {
        it('Should produce same date when converting TZ -12:00', () => {
            expect(formatDate(new Date('1995-05-13T00:00:00-12:00'))).toEqual('1995-05-13');
        });

        it('Should produce previous date when converting TZ +01:00', () => {
            expect(formatDate(new Date('1995-05-13T00:00:00+01:00'))).toEqual('1995-05-12');
        });
    });
});
