import {spread} from '../src';
import {expectType} from './utils';

describe('spread()', () => {
    it('корректно работает с разными типами входных данных', () => {
        const result = spread([
            Promise.resolve('Hello, world!'),
            42,
            new Promise<string>(resolve => setTimeout(resolve, 100, 'Hello, world!')),
        ],
        (res1, res2, res3) => {
            expect(res1).toEqual('Hello, world!');
            expectType<string>(res1);
            expect(res2).toEqual(42);
            expectType<number>(res2);
            expect(res3).toEqual('Hello, world!');
            expectType<string>(res3);

            return [res1, res2, res3, res2, res1] as const;
        });
        expectType<Promise<readonly [string, number, string, number, string]>>(result);
    });
});
