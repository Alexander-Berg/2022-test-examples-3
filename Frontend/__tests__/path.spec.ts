import { URL } from 'url';
import { getUrlPath, filterQueryParams } from '../path';

describe('getUrlPath', () => {
    const url = 'https://project.ru:8080/path/to';

    it('должен корректно строить URL из объекта', () => {
        const builtURL = getUrlPath(url, { a: '1', b: '2', c: ['1', '2'] });

        expect(builtURL).toEqual(`${url}?a=1&b=2&c=1&c=2`);
    });

    it('должен падать с ошибкой при передаче не валидного URL', () => {
        try {
            getUrlPath('path/to', { a: '1', b: '2', c: ['1', '2'] });
        } catch (e) {
            expect(true).toEqual(true);
        }
    });

    it('фильтрует пустые или не заданные значения', () => {
        const builtURL = getUrlPath(url, { a: '0', b: undefined, d: '', e: ['1', '', '2'] });

        expect(builtURL).toEqual(`${url}?a=0&e=1&e=2`);
    });
});

describe('filterQueryParams', () => {
    describe('url яляется строкой', () => {
        it('должен возвращать пустой объект на пустых query params', () => {
            const url = 'https://project.ru:8080/path/to';
            const params = filterQueryParams(url, ['one', 'two', 'three']);

            expect(params).toEqual({});
        });

        it('должен возвращать query params что прошли фильтрацию', () => {
            const url = 'https://project.ru:8080/path/to?one=1&dd=1&two=2&two=5&three=3&foure=4';
            const params = filterQueryParams(url, ['one', 'two', 'three']);

            expect(params).toEqual({ one: '1', two: ['2', '5'], three: '3' });
        });
    });

    describe('url является экземпляром URL', () => {
        it('должен возвращать пустой объект на пустых query params', () => {
            const url = new URL('https://project.ru:8080/path/to');
            const params = filterQueryParams(url, ['one', 'two', 'three']);

            expect(params).toEqual({});
        });

        it('должен возвращать query params что прошли фильтрацию', () => {
            const url = new URL('https://project.ru:8080/path/to?one=1&dd=1&two=2&two=5&three=3&foure=4');
            const params = filterQueryParams(url, ['one', 'two', 'three']);

            expect(params).toEqual({ one: '1', two: ['2', '5'], three: '3' });
        });
    });
});
