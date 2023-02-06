import { delay, timeout, debounce } from './time';

describe('time', () => {
    describe('timeout', () => {
        it('Should return sleep promise', async() => {
            const now = Date.now();
            const { sleep } = timeout(200);

            await sleep;

            expect(Date.now() - now).toBeGreaterThanOrEqual(200);
        });

        it('Should support function that calls before resolution', async() => {
            const spy: number[] = [];
            const { sleep } = timeout(0, () => {
                spy.push(1);
            });

            sleep.then(() => {
                spy.push(2);
            });

            await sleep;

            expect(spy).toEqual([1, 2]);
        });

        it('Should also return clear function', async() => {
            const spy = jest.fn();
            const { sleep, clear } = timeout(200);

            sleep.then(spy);

            clear();

            await new Promise(resolve => {
                setTimeout(resolve, 500);
            });

            expect(spy).not.toHaveBeenCalled();
        });

        it('Should also return awake function', async() => {
            const spy = jest.fn();
            const { sleep, awake } = timeout(1000000, spy);

            sleep.then(spy);

            awake();

            await sleep;

            expect(spy).toHaveBeenCalled();
        });
    });

    describe('delay', () => {
        it('Should return sleep promise', async() => {
            const now = Date.now();

            await delay(500);

            expect(Date.now() - now).toBeGreaterThanOrEqual(500);
        });
    });

    describe('debounce', () => {
        it('Should debounce callback', async() => {
            const spy = jest.fn();
            const fn = debounce(500, spy);

            fn();
            fn();
            fn();
            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 100);
            });

            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 100);
            });

            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 100);
            });

            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 100);
            });

            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 100);
            });

            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 100);
            });

            fn();

            await new Promise(resolve => {
                setTimeout(resolve, 700);
            });

            expect(spy).toHaveBeenCalledTimes(1);
        });
    });
});
