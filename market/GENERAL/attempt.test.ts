import {attempt} from '../src';
import {expectType} from './utils';

describe('attempt()', () => {
    it('корректно работает с разными коллбеками', () => {
        const callbackString = (): string => 'Hello, world';
        const callbackNumber = (): number => 42;
        const callbackObject = (): {num: number} => ({num: 42});
        const callbackFunction = (): ((num: number) => number) => num => num;

        attempt(callbackString).then((str: string) => {
            expect(str).toEqual('Hello, world');
            expectType<string>(str);
        });

        attempt(callbackNumber).then((num: number) => {
            expect(num).toEqual(42);
            expectType<number>(num);
        });

        attempt(callbackObject).then((obj: {num: number}) => {
            expect(obj.num).toEqual(42);
            expectType<{num: number}>(obj);
        });

        attempt(callbackFunction).then((func: (num: number) => number) => {
            const result = func(42);
            expect(result).toEqual(42);
            expectType<(n: number) => number>(func);
        });
    });
});
