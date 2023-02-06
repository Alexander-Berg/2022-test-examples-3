import { get } from '../get';

describe('get', () => {
    it('Вернёт существующее значение, если путь существует', () => {
        const value = get({ a: { b: { c: 'foo' } } }, 'a.b.c');

        expect(value).toBe('foo');
    });

    it('Вернёт существующее значение, если путь существует, в пути есть индекс массива', () => {
        const value = get({ a: [undefined, { b: { c: 'foo' } }] }, 'a.1.b.c');

        expect(value).toBe('foo');
    });

    it('Вернёт undefined, если пути нет', () => {
        const value = get({ a: { b: { c: 'foo' } } }, 'z.y.x');

        expect(value).toBeUndefined();
    });

    it('Вернёт дефолтное значение, если пути нет и значение задано', () => {
        const value = get({ a: { b: { c: 'foo' } } }, 'z.y.x', 'bar');

        expect(value).toBe('bar');
    });
});
