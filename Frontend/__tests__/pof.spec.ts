import { getPof } from '../pof';

describe('getPof', () => {
    it(
        'Если функция не получила mclid или clid, то возвращает null',
        () => {
            const searchParams = new URLSearchParams('a=1&b=2');
            const expected = null;
            const actual = getPof(searchParams);

            expect(actual).toEqual(expected);
        }
    );

    it(
        'Если функция получила только валидный mclid, ' +
        'то возвращает объект с нужными ключами, где clid пустой массив',
        () => {
            const mclid = '1';
            const searchParams = new URLSearchParams(`mclid=${mclid}`);
            const expected = {
                clid: [],
                mclid,
                vid: null,
                distr_type: null,
                opp: null,
            };
            const actual = getPof(searchParams);

            expect(actual).toEqual(expected);
        }
    );

    it(
        'Если функция получила только невалидный mclid, то возвращает null',
        () => {
            const mclid = 'a';
            const searchParams = new URLSearchParams(`mclid=${mclid}`);
            const expected = null;
            const actual = getPof(searchParams);

            expect(actual).toEqual(expected);
        }
    );

    it(
        'Если функция получила валидный clid, то возвращает объект с нужными ключами, где clid массив',
        () => {
            const clid = '1';
            const searchParams = new URLSearchParams(`clid=${clid}`);
            const expected = {
                clid: [clid],
                mclid: null,
                vid: null,
                distr_type: null,
                opp: null,
            };
            const actual = getPof(searchParams);

            expect(actual).toEqual(expected);
        }
    );

    it(
        'Если функция получила только невалидный clid, то он фильтруется, и результат функции null',
        () => {
            const clid = 'abc';
            const searchParams = new URLSearchParams(`clid=${clid}`);
            const expected = null;
            const actual = getPof(searchParams);

            expect(actual).toEqual(expected);
        }
    );

    it(
        'Если функция получила больше трёх валидных clid,' +
        'то возвращает объект с нужными ключами, где clid массив, который обрезан до двух значений',
        () => {
            const clid = ['1', '2', '3'];
            const searchParams = new URLSearchParams(`clid=${clid.join('&clid=')}`);
            const expected = {
                clid: [...clid.slice(0, 2)],
                mclid: null,
                vid: null,
                distr_type: null,
                opp: null,
            };
            const actual = getPof(searchParams);

            expect(actual).toEqual(expected);
        }
    );
});
