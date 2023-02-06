import {props} from '../src';
import {expectType} from './utils';

describe('props()', () => {
    const resolvedObj = {
        firstPromise: 'Hello, world!',
        secondPromise: 42,
    };

    it('корректно работает с промисами', async () => {
        const obj = {
            firstPromise: Promise.resolve('Hello, world!'),
            secondPromise: Promise.resolve(42),
        };

        const result = await props(obj)
        
        expect(result).toEqual(resolvedObj);
        expectType<{firstPromise: string, secondPromise: number}>(result);
    });

    it('корректно работает не с промисами', async () => {
        const obj = {
            firstPromise: Promise.resolve('Hello, world!'),
            secondPromise: 42,
        };

        const result = await props(obj);
        expect(result).toEqual(resolvedObj);
        expectType<{firstPromise: string, secondPromise: number}>(result);
    });
});
