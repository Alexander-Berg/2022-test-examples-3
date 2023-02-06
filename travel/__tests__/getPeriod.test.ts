import {getDaysPeriod} from 'utilities/dateUtils/getPeriod';

describe('getDaysPeriod', () => {
    it('Вернёт пустой период, если from > to', () => {
        expect(
            getDaysPeriod({
                from: '2021-01-22',
                to: '2021-01-20',
            }),
        ).toEqual([]);
    });

    it('Вернёт пустой период из одного элемента, если from = to', () => {
        expect(
            getDaysPeriod({
                from: '2021-01-22',
                to: '2021-01-22',
            }),
        ).toEqual(['2021-01-22']);
    });

    it('Вернёт период соответствующей длины, если from > to', () => {
        expect(
            getDaysPeriod({
                from: '2021-01-22',
                to: '2021-01-24',
            }),
        ).toEqual(['2021-01-22', '2021-01-23', '2021-01-24']);
        expect(
            getDaysPeriod({
                from: '2021-01-31',
                to: '2021-02-01',
            }),
        ).toEqual(['2021-01-31', '2021-02-01']);
    });
});
