import { humanize } from './DateFromNow';
import i18n from '../../../shared/lib/i18n';
import * as en from '../../../langs/yamb/en.json';
import * as ru from '../../../langs/yamb/ru.json';

const time = (date: string): number => {
    const d = new Date(`${date.replace(' ', 'T')}Z`);

    return d.getTime();
};

describe('humanize', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('ранее, чем за 11 месяцев', () => {
            expect(humanize(time('1990-11-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('27 years ago');
            expect(humanize(time('2014-11-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('3 years ago');
            expect(humanize(time('2016-11-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 year ago');
            expect(humanize(time('2017-02-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 year ago');
        });

        it('ранее, чем за 26 дней', () => {
            expect(humanize(time('2017-04-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('9 months ago');
            expect(humanize(time('2017-11-10 00:00:00') - time('2018-01-02 00:00:00'))).toBe('2 months ago');
            expect(humanize(time('2017-12-02 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 month ago');
        });

        it('ранее, чем за 22 часа', () => {
            expect(humanize(time('2017-12-22 00:00:00') - time('2018-01-02 00:00:00'))).toBe('11 days ago');
            expect(humanize(time('2018-01-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 day ago');
        });

        it('ранее, чем за 45 минут', () => {
            expect(humanize(time('2018-01-01 23:00:00') - time('2018-01-02 00:00:00'))).toBe('1 hour ago');
        });

        it('ранее, чем за 45 секунд', () => {
            expect(humanize(time('2018-01-01 23:58:50') - time('2018-01-02 00:00:00'))).toBe('1 minute ago');
            expect(humanize(time('2018-01-01 23:58:59') - time('2018-01-02 00:00:00'))).toBe('1 minute ago');
            expect(humanize(time('2018-01-01 23:59:00') - time('2018-01-02 00:00:00'))).toBe('1 minute ago');
            expect(humanize(time('2018-01-01 23:59:14') - time('2018-01-02 00:00:00'))).toBe('1 minute ago');
        });

        it('не ранее, чем за 45 секунд', () => {
            expect(humanize(time('2018-01-01 23:59:50') - time('2018-01-02 00:00:00'))).toBe('a few seconds ago');
            expect(humanize(time('2018-01-01 23:59:59') - time('2018-01-02 00:00:00'))).toBe('a few seconds ago');
        });

        it('сейчас', () => {
            expect(humanize(time('2018-01-02 00:00:00') - time('2018-01-02 00:00:00'))).toBe('just now');
        });

        it('ранее, чем через 45 секунд', () => {
            expect(humanize(time('2018-01-02 00:00:01') - time('2018-01-02 00:00:00'))).toBe('in a few seconds');
            expect(humanize(time('2018-01-02 00:00:10') - time('2018-01-02 00:00:00'))).toBe('in a few seconds');
        });

        it('не ранее, чем через 45 секунд', () => {
            expect(humanize(time('2018-01-02 00:00:59') - time('2018-01-02 00:00:00'))).toBe('in 1 minute');
            expect(humanize(time('2018-01-02 00:01:00') - time('2018-01-02 00:00:00'))).toBe('in 1 minute');
            expect(humanize(time('2018-01-02 00:02:00') - time('2018-01-02 00:00:00'))).toBe('in 2 minutes');
            expect(humanize(time('2018-01-02 00:10:00') - time('2018-01-02 00:00:00'))).toBe('in 10 minutes');
            expect(humanize(time('2018-01-02 00:20:00') - time('2018-01-02 00:00:00'))).toBe('in 20 minutes');
        });

        it('не ранее, чем через 45 минут', () => {
            expect(humanize(time('2018-01-02 00:50:00') - time('2018-01-02 00:00:00'))).toBe('in 1 hour');
            expect(humanize(time('2018-01-02 01:00:00') - time('2018-01-02 00:00:00'))).toBe('in 1 hour');
            expect(humanize(time('2018-01-02 10:00:00') - time('2018-01-02 00:00:00'))).toBe('in 10 hours');
        });

        it('не ранее, чем через 22 часа', () => {
            expect(humanize(time('2018-01-03 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 1 day');
            expect(humanize(time('2018-01-06 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 4 days');
            expect(humanize(time('2018-01-11 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 9 days');
        });

        it('не ранее, чем через 26 дней', () => {
            expect(humanize(time('2018-01-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 1 month');
            expect(humanize(time('2018-03-03 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 2 months');
        });

        it('не ранее, чем через 11 месяцев', () => {
            expect(humanize(time('2018-11-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 1 year');
            expect(humanize(time('2019-11-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 2 years');
            expect(humanize(time('2032-11-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('in 15 years');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('ранее, чем за 11 месяцев', () => {
            expect(humanize(time('1990-11-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('27 лет назад');
            expect(humanize(time('2014-11-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('3 года назад');
            expect(humanize(time('2016-11-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 год назад');
            expect(humanize(time('2017-02-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 год назад');
        });

        it('ранее, чем за 26 дней', () => {
            expect(humanize(time('2017-04-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('9 месяцев назад');
            expect(humanize(time('2017-11-10 00:00:00') - time('2018-01-02 00:00:00'))).toBe('2 месяца назад');
            expect(humanize(time('2017-12-02 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 месяц назад');
        });

        it('ранее, чем за 22 часа', () => {
            expect(humanize(time('2017-12-22 00:00:00') - time('2018-01-02 00:00:00'))).toBe('11 дней назад');
            expect(humanize(time('2017-12-12 00:00:00') - time('2018-01-02 00:00:00'))).toBe('21 день назад');
            expect(humanize(time('2018-01-01 00:00:00') - time('2018-01-02 00:00:00'))).toBe('1 день назад');
        });

        it('ранее, чем за 45 минут', () => {
            expect(humanize(time('2018-01-01 23:00:00') - time('2018-01-02 00:00:00'))).toBe('1 час назад');
        });

        it('ранее, чем за 45 секунд', () => {
            expect(humanize(time('2018-01-01 23:58:50') - time('2018-01-02 00:00:00'))).toBe('1 минуту назад');
            expect(humanize(time('2018-01-01 23:58:59') - time('2018-01-02 00:00:00'))).toBe('1 минуту назад');
            expect(humanize(time('2018-01-01 23:59:00') - time('2018-01-02 00:00:00'))).toBe('1 минуту назад');
            expect(humanize(time('2018-01-01 23:59:14') - time('2018-01-02 00:00:00'))).toBe('1 минуту назад');
        });

        it('не ранее, чем за 45 секунд', () => {
            expect(humanize(time('2018-01-01 23:59:50') - time('2018-01-02 00:00:00'))).toBe('несколько секунд назад');
            expect(humanize(time('2018-01-01 23:59:59') - time('2018-01-02 00:00:00'))).toBe('несколько секунд назад');
        });

        it('сейчас', () => {
            expect(humanize(time('2018-01-02 00:00:00') - time('2018-01-02 00:00:00'))).toBe('только что');
        });

        it('ранее, чем через 45 секунд', () => {
            expect(humanize(time('2018-01-02 00:00:01') - time('2018-01-02 00:00:00'))).toBe('через несколько секунд');
            expect(humanize(time('2018-01-02 00:00:10') - time('2018-01-02 00:00:00'))).toBe('через несколько секунд');
        });

        it('не ранее, чем через 45 секунд', () => {
            expect(humanize(time('2018-01-02 00:00:59') - time('2018-01-02 00:00:00'))).toBe('через 1 минуту');
            expect(humanize(time('2018-01-02 00:01:00') - time('2018-01-02 00:00:00'))).toBe('через 1 минуту');
            expect(humanize(time('2018-01-02 00:02:00') - time('2018-01-02 00:00:00'))).toBe('через 2 минуты');
            expect(humanize(time('2018-01-02 00:10:00') - time('2018-01-02 00:00:00'))).toBe('через 10 минут');
            expect(humanize(time('2018-01-02 00:20:00') - time('2018-01-02 00:00:00'))).toBe('через 20 минут');
            expect(humanize(time('2018-01-02 00:21:00') - time('2018-01-02 00:00:00'))).toBe('через 21 минуту');
            expect(humanize(time('2018-01-02 00:31:00') - time('2018-01-02 00:00:00'))).toBe('через 31 минуту');
        });

        it('не ранее, чем через 45 минут', () => {
            expect(humanize(time('2018-01-02 00:50:00') - time('2018-01-02 00:00:00'))).toBe('через 1 час');
            expect(humanize(time('2018-01-02 01:00:00') - time('2018-01-02 00:00:00'))).toBe('через 1 час');
            expect(humanize(time('2018-01-02 10:00:00') - time('2018-01-02 00:00:00'))).toBe('через 10 часов');
        });

        it('не ранее, чем через 22 часа', () => {
            expect(humanize(time('2018-01-03 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 1 день');
            expect(humanize(time('2018-01-06 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 4 дня');
            expect(humanize(time('2018-01-11 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 9 дней');
        });

        it('не ранее, чем через 26 дней', () => {
            expect(humanize(time('2018-01-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 1 месяц');
            expect(humanize(time('2018-03-03 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 2 месяца');
        });

        it('не ранее, чем через 11 месяцев', () => {
            expect(humanize(time('2018-11-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 1 год');
            expect(humanize(time('2019-11-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 2 года');
            expect(humanize(time('2032-11-31 00:00:00') - time('2018-01-02 00:00:00'))).toBe('через 15 лет');
        });
    });
});
