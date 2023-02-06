import { getPromoMeta, computeProgress, computeTimeToEnd, getTillEndTimeDescription } from '../fromVhVideoObject';

describe('Функция getPromoMeta', () => {
    it.each([
        [{}, ''],
        [
            { release_year: 2015, genres: ['Комедия', 'Криминал'], rating_kp: 7.9888882 },
            '8.0 · 2015, Комедия, Криминал',
        ],
        [
            {
                release_year: 2015,
                genres: ['Комедия', 'Криминал'],
                rating_kp: 7.2111111,
                countries: 'США',
            },
            '7.2 · 2015, Комедия, Криминал · США',
        ],
        [
            {
                release_year: 2015,
                genres: ['Комедия', 'Криминал', 'Ужасы', 'Какой-то ще жанр'],
                rating_kp: 7.9888882,
                countries: 'США, Россия',
                includes: [{ season: { season_number: 1 } }],
            },
            '8.0 · 2015, Комедия, Криминал, Ужасы · 1 сезон · США, Россия',
        ],
    ])('должна возвращать ожидаемый результат', (item, expected) => {
        expect(getPromoMeta(item)).toEqual(expected);
    });
});

describe('Функция computeProgress', () => {
    it('должна возвращать ожидаемый результат', () => {
        expect(computeProgress(10000, 9850)).toBe(99);
        expect(computeProgress(10000, 5000)).toBe(50);
        expect(computeProgress(10000, 9999)).toBe(100);
        expect(computeProgress(10000, 101)).toBe(2);
        expect(computeProgress(10000, 1)).toBe(1);
        expect(computeProgress(10000, 0)).toBe(0);
        expect(computeProgress(10000, 12000)).toBe(100);
    });
});

describe('Функция computeTimeToEnd', () => {
    it('должна возвращать ожидаемый результат', () => {
        expect(computeTimeToEnd()).toEqual(undefined);
        expect(computeTimeToEnd(120 * 60, 38 * 60)).toEqual(82);
        expect(computeTimeToEnd(120 * 60, 120 * 60)).toEqual(0);
        expect(computeTimeToEnd(120 * 60, 120 * 60 - 59)).toEqual(0);
        expect(computeTimeToEnd(120 * 60, 120 * 60 - 60)).toEqual(1);
        expect(computeTimeToEnd(120 * 60, 140 * 60)).toEqual(undefined);
    });
});

describe('Функция getTillEndTimeDescription', () => {
    it('должна возвращать ожидаемый результат', () => {
        expect(getTillEndTimeDescription()).toEqual(undefined);
        expect(getTillEndTimeDescription(5)).toEqual('Осталось 5 минут');
        expect(getTillEndTimeDescription(60)).toEqual('Осталось 60 минут');
        expect(getTillEndTimeDescription(0)).toEqual('Осталось меньше минуты');
        expect(getTillEndTimeDescription(1)).toEqual('Осталась 1 минута');
        expect(getTillEndTimeDescription(61)).toEqual('Остался 1 час 1 минута');
        expect(getTillEndTimeDescription(130)).toEqual('Осталось 2 часа 10 минут');
    });
});
