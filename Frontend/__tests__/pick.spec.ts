import { pick } from '../pick';

describe('pick', () => {
    it('Вернёт объект только с нужными свойствами', () => {
        const obj = pick({ a: 'a', b: 'b', c: 'c' }, ['a', 'b']);

        expect(obj.hasOwnProperty('a')).toBeTruthy();
        expect(obj.hasOwnProperty('b')).toBeTruthy();
        expect(obj.hasOwnProperty('c')).toBeFalsy();
    });
});
