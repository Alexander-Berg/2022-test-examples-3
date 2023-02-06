import {expectType} from '../helpers';
import {find} from '..';

describe('find', () => {
    it('should throw exception if second argument not object or array', () => {
        // @ts-expect-error
        expect(() => find(x => Boolean(x), '')).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => find(x => Boolean(x), null)).toThrow();
    });

    it('should work with array', () => {
        const collection = [1, 2, 3];

        expect(find(x => x === 2, collection)).toBe(2);
        expect(find(x => x === 2)(collection)).toBe(2);
        expect(find(x => x === 42, collection)).toBe(undefined);
        expect(find(x => x === 42)(collection)).toBe(undefined);
    });

    it('should work with object', () => {
        const collection = {foo: 1, bar: 2};

        expect(find(x => x === 2, collection)).toBe(2);
        expect(find(x => x === 2)(collection)).toBe(2);
        expect(find(x => x === 42, collection)).toBe(undefined);
        expect(find(x => x === 42)(collection)).toBe(undefined);
    });

    it('has right types for arrays', () => new Promise<void>(done => {
        const collection = [1, 2, 3];

        const cb = (x: number, i: number, a: readonly number[]) => (2 * x) + (2 * i) + a.length === 2;

        // @ts-expect-error Wrong type of value
        find((x: string, i: number, a: readonly number[]) => true, collection);
        // @ts-expect-error Wrong type of index
        find((x: number, i: string, a: readonly number[]) => true, collection);
        // @ts-expect-error Wrong type of collection
        find((x: number, i: number, a: readonly string[]) => true, collection);

        expectType<number | undefined>(find(cb, collection));
        expectType<number | undefined>(find(cb)(collection));

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<number>(find(cb, collection));
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<number>(find(cb)(collection));

        done();
    }));

    it('has right types for objects', () => new Promise<void>(done => {
        const collection = {foo: 1, bar: 42, zoo: 0};

        const cb = (x: number, k: string, o: Readonly<typeof collection>) => x + o.foo === 2;

        find(cb, collection);
        // @ts-expect-error Wrong type of value
        find((x: string, k: string, o: Readonly<typeof collection>) => x + o.foo === 2, collection);
        // @ts-expect-error Wrong type of index
        find((x: number, k: number, o: Readonly<typeof collection>) => x + o.foo === 2, collection);
        // @ts-expect-error Wrong type of collection
        find((x: number, k: string, o: {foo: 'bar'}) => x + o.foo === 2, collection);

        expectType<number | undefined>(find(cb, collection));
        expectType<number | undefined>(find(cb)(collection));

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<number>(find(cb, collection));
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<number>(find(cb)(collection));

        done();
    }));
});
