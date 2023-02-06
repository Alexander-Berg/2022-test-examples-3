import {waitUntil} from './waitUntil';

const controlledPredicate = (timeout = 100, arg: 'resolve' | 'reject') => {
    let done = false;
    setTimeout(() => {
        done = arg === 'resolve' ? true : false;
    }, timeout);

    return () => done;
};

const sleep = (timeout: number) =>
    new Promise(resolve => setTimeout(resolve, timeout));

describe('waitUntilPr', () => {
    test('Should correct stop promise if canceled', async () => {
        const prFn = controlledPredicate(5, 'resolve');

        const doneCb = jest.fn();
        const errorCb = jest.fn();

        // Можно сделать before each
        const cancel = waitUntil({
            predicate: prFn,
            done: doneCb,
            error: errorCb,
            pollTime: 3,
            until: 10,
        });
        if (cancel) cancel();
        expect(errorCb).toBeCalledTimes(1);

        await sleep(10);
        expect(doneCb).not.toBeCalled();
        expect(errorCb).toBeCalledTimes(1);
    });
    test('Should correct resolved after expect timeout', async () => {
        const prFn = controlledPredicate(5, 'resolve');

        const doneCb = jest.fn();
        const errorCb = jest.fn();

        waitUntil({
            predicate: prFn,
            done: doneCb,
            error: errorCb,
            pollTime: 3,
            until: 10,
        });

        expect(doneCb).not.toBeCalled();

        await sleep(10);

        expect(doneCb).toBeCalledTimes(1);
        expect(errorCb).not.toBeCalled();
    });
    test('Should correct rejected after expect timeout', async () => {
        const prFn = controlledPredicate(5, 'reject');

        const doneCb = jest.fn();
        const errorCb = jest.fn();

        waitUntil({
            predicate: prFn,
            done: doneCb,
            error: errorCb,
            pollTime: 3,
            until: 10,
        });

        expect(errorCb).not.toBeCalled();

        await sleep(10);

        expect(doneCb).not.toBeCalled();
        expect(errorCb).toBeCalledTimes(1);
    });
});
