import {map} from '..';

describe('map', () => {
    const array = [1, 2];
    // eslint-disable-next-line
    const falsey = [, null, undefined, false, 0, NaN, ''];

    it('has right types for arrays', () => new Promise<void>(done => {
        const arr = [1, 3, 5, 7];
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 should be inferred?
        const cb = (val: number, idx: number, a: number[]) => {
            const _val: number = val;
            // @ts-expect-error
            const _badVal: string = val;

            const _idx: number = idx;
            // @ts-expect-error
            const _badIdx: string = idx;

            const _a: ReadonlyArray<number> = a;
            // @ts-expect-error
            const _badA: string[] = a;

            return _val + _badVal + _idx + _badIdx + _a.length + _badA.length;
        };

        map(cb, arr);

        done();
    }));

    it('has troubles with types in curried version for arrays', () => new Promise<void>(done => {
        const arr = [1, 3, 5, 7];
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 should be inferred?
        const cb = (val: number, idx: number, a: number[]) => {
            const _val: number = val;
            // @ts-expect-error
            const _badVal: string = val;

            const _idx: number = idx;

            const _a: ReadonlyArray<number> = a;
            // @ts-expect-error
            const _badA: string[] = a;

            return _val + _badVal + _idx + _a.length + _badA.length;
        };

        map(cb)(arr);

        done();
    }));

    it('has right types for objects', () => new Promise<void>(done => {
        const obj = {foo: 42, bar: 17};
        const obj2 = {qwe: 42, asd: 17};
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 should be inferred?
        const cb = (val: number, key: string, o: typeof obj) => {
            const _val: number = val;
            // @ts-expect-error
            const _badVal: string = val;

            const _key: string = key;
            // @ts-expect-error
            const _badKey: number = key;

            const _o: Readonly<typeof obj> = o;
            // @ts-expect-error
            const _badO: string[] = o;

            return _val + _badVal + _key + _badKey + _o.foo + _badO.length;
        };

        map(cb, obj);
        // @ts-expect-error
        map(cb, obj2);

        done();
    }));

    it('has troubles with types for objects in curried version', () => new Promise<void>(done => {
        const obj = {foo: 42, bar: 17};
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 should be inferred?
        const cb = (val: number, key: string, o: typeof obj) => {
            const _val: number = val;
            // @ts-expect-error
            const _badVal: string = val;

            const _key: string = key;
            // @ts-expect-error
            const _badKey: number = key;

            const _o: Readonly<typeof obj> = o;
            // @ts-expect-error
            const _badO: string[] = o;

            return _val + _badVal + _key + _badKey + _o.foo + _badO.length;
        };

        // @ts-expect-error curry
        map(cb)(obj);

        done();
    }));

    it('should map values in `collection` to a new array', () => {
        const object = {a: 1, b: 2};
        const expected = ['1', '2'];

        expect(map(String, array)).toEqual(expected);
        expect(map(String)(array)).toEqual(expected);
        expect(map(String, object)).toEqual(expected);
        expect(map(String)(object)).toEqual(expected);
    });

    it('should iterate over own string keyed properties of objects', () => {
        function Foo() {
            this.a = 1;
        }
        Foo.prototype.b = 2;

        expect(map(x => x, new Foo())).toEqual([1]);
    });

    it('should not accept a falsey `collection`', () => {
        for (const falsy of falsey) {
            // @ts-expect-error
            expect(() => map(x => x, falsy)).toThrow();
        }
    });

    it('should not accept primitives', () => {
        // @ts-expect-error
        expect(() => map(x => x, 'abcde')).toThrow();
        // @ts-expect-error
        expect(() => map(x => x, 1)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => map(x => x, null)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => map(x => x, undefined)).toThrow();
    });

    it('should work with objects with non-number length properties', () => {
        const value = {value: 'x'};
        const object = {length: {value: 'x'}};

        expect(map(x => x, object)).toEqual([value]);
    });
});
