import { queryStringify, isYandexTeamHost } from '../url';

describe('#queryStringify', () => {
    it('должен добавлять параметры', () => {
        const actual = queryStringify('http://ya.ru', {
            a: 1,
            b: 'hello',
        });

        expect(actual).toBe('http://ya.ru?a=1&b=hello');
    });

    it('должен добавлять параметры, учитывая ?', () => {
        const actual = queryStringify('http://ya.ru?c=2', {
            a: 1,
            b: 'hello',
        });

        expect(actual).toBe('http://ya.ru?c=2&a=1&b=hello');
    });

    it('должен экранировать значение параметров', () => {
        const actual = queryStringify('http://ya.ru?c=2', {
            a: 'a b c',
        });

        expect(actual).toBe('http://ya.ru?c=2&a=a%20b%20c');
    });

    it('должен поддерживать параметры как массивы', () => {
        const actual = queryStringify('http://ya.ru?c=2', {
            a: ['a', 'b', 'c'],
            b: 'abc',
        });

        expect(actual).toBe('http://ya.ru?c=2&a=a&a=b&a=c&b=abc');
    });

    it('не должен добавлять пустые параметры', () => {
        const actual = queryStringify('http://ya.ru?c=2', {
            a: 1,
            b: null,
            c: undefined,
            d: '',
        });

        expect(actual).toBe('http://ya.ru?c=2&a=1');
    });
});

describe('#isYandexTeamHost', () => {
    it('должен вернуть true для staff.yandex-team.ru', () => {
        expect(isYandexTeamHost('staff.yandex-team.ru')).toBeTruthy();
    });

    it('должен вернуть false для example.com', () => {
        expect(isYandexTeamHost('example.com')).toBeFalsy();
    });
});
