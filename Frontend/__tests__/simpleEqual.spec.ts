import { simpleEqual } from '../simpleEqual';

describe('simpleEqual', () => {
    it('Объекты равны', () => {
        const foo = { key: 'value' };
        const bar = { key: 'value' };

        expect(simpleEqual(foo, bar)).toBeTruthy();
    });

    it('Объекты тождественно равны', () => {
        const foo = { key: 'value' };
        const bar = foo;

        expect(simpleEqual(foo, bar)).toBeTruthy();
    });

    it('Объекты не равны, если значения ключей разное', () => {
        const foo = { key: 'value' };
        const bar = { key: 'валуе' };

        expect(simpleEqual(foo, bar)).toBeFalsy();
    });

    it('Объекты не равны, если количество ключей разное', () => {
        const foo = { key: 'value' };
        const bar = { };

        expect(simpleEqual(foo, bar)).toBeFalsy();
    });

    it('Объекты не равны, если в одном объекте нет какого-то ключа', () => {
        const foo = { key: 'value' };
        const bar = { yek: 'eulav' };

        expect(simpleEqual(foo, bar)).toBeFalsy();
    });
});
