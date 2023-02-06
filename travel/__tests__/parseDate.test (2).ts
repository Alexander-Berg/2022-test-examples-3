import {momentTimezone as moment} from '../../../../reexports';

import {ROBOT} from '../formats';

import Lang from '../../../interfaces/Lang';

import parseDate from '../parseDate';

const language = Lang.ru;
const moment20160217 = moment.tz(1455662700000, 'Asia/Yekaterinburg'); // 2016-02-17T03:45:00+05:00

describe('parseDate', () => {
    describe('month number', () => {
        it('01.01', () => {
            const result = parseDate('01.01', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2017-01-01');
        });

        it('29-02', () => {
            const result = parseDate('29.02', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-02-29');
        });

        it('31/12', () => {
            const result = parseDate('31/12', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-12-31');
        });

        it('31.02', () => {
            const result = parseDate('31.02', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('01.15', () => {
            const result = parseDate('01.15', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('00.03', () => {
            const result = parseDate('00.03', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('10.00', () => {
            const result = parseDate('10.00', moment20160217, language);

            expect(result).toBeUndefined();
        });
    });

    describe('month name', () => {
        it('1 января', () => {
            const result = parseDate('01 янва', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2017-01-01');
        });

        it('29 февраля', () => {
            const result = parseDate('29 фев', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-02-29');
        });

        it('31 декабря', () => {
            const result = parseDate('31 дека', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-12-31');
        });

        it('12марта (без пробела)', () => {
            const result = parseDate('12мар', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-03-12');
        });

        it('31 февраля', () => {
            const result = parseDate('31 февра', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('1 феееевраля', () => {
            const result = parseDate('01 феееевра', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('0 марта', () => {
            const result = parseDate('0 мар', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('1 января 2017', () => {
            const result = parseDate('1 января 2017', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2017-01-01');
        });

        it('29 февраля 2020', () => {
            const result = parseDate(
                '29 февраля',
                moment.tz(1573631466220, 'Asia/Yekaterinburg'),
                language,
            ); // 2019-11-13

            expect(result && result.format(ROBOT)).toBe('2020-02-29');
        });
    });

    describe('no month', () => {
        it('1', () => {
            const result = parseDate('1', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-02-17');
        });

        it('2', () => {
            const result = parseDate('2', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-02-20');
        });

        it('3', () => {
            const result = parseDate('3', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-03-03');
        });

        it('29', () => {
            const result = parseDate('29', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-02-29');
        });

        it('31', () => {
            const result = parseDate('31', moment20160217, language);

            expect(result && result.format(ROBOT)).toBe('2016-03-31');
        });

        it('32', () => {
            const result = parseDate('32', moment20160217, language);

            expect(result).toBeUndefined();
        });

        it('0', () => {
            const result = parseDate('0', moment20160217, language);

            expect(result).toBeUndefined();
        });
    });

    describe('invalid text', () => {
        it('qwerty', () => {
            const result = parseDate('qwerty', moment20160217, language);

            expect(result).toBeUndefined();
        });
    });
});
