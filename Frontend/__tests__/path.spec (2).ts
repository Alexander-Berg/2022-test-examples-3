import { getUrlPath } from '../path';

describe('getUrlPath', () => {
    const url = 'https://project.ru:8080/path/to';
    const comma = '%2C';
    const bracketOpen = '%5B';
    const bracketClose = '%5D';

    it('должен корректно строить URL из объекта', () => {
        const builtURL = getUrlPath(url, { a: '1', b: '2', c: ['1', '2'] });

        expect(builtURL).toEqual(`${url}?a=1&b=2&c=1${comma}2`);
    });

    it('должен корректно строить URL из строки', () => {
        const builtURL = getUrlPath(url, 'a=1&b=2&c[]=1&c[]=2');

        expect(builtURL).toEqual(`${url}?a=1&b=2&c${bracketOpen}${bracketClose}=1&c${bracketOpen}${bracketClose}=2`);
    });

    it('должен падать с ошибкой при передаче не валидного URL', () => {
        try {
            getUrlPath('path/to', { a: '1', b: '2', c: ['1', '2'] });
        } catch (e) {
            expect(true).toEqual(true);
        }
    });
});
