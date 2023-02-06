import { cancelable, CancelablePromise, ABORT_TOKEN } from '../cancelable';

type PatchedPromise<P> = Promise<P> & { resolve(r: P): void; reject(r?: any): void };

function getPromise<P>(): PatchedPromise<P> {
    let doResolve;
    let doReject;

    const promise = new Promise<P>((resolve, reject) => {
        doResolve = resolve;
        doReject = reject;
    }) as PatchedPromise<P>;

    promise.resolve = doResolve;
    promise.reject = doReject;

    return promise;
}

describe(('cancelable promise'), () => {
    describe('#cancelable', () => {
        test('Should be canceled', () => {
            const cancelableFn = cancelable(() => getPromise<string>());
            const result = cancelableFn();

            cancelableFn.cancel();

            expect(result).rejects.toBe(ABORT_TOKEN);
        });

        test('First request should be canceled', () => {
            const cancelableFn = cancelable(() => getPromise<string>());
            const result = cancelableFn();

            cancelableFn();

            return expect(result).rejects.toBe(ABORT_TOKEN);
        });
    });

    describe('#CancelablePromise', () => {
        test('Fullfield promise should not be canceled', () => {
            const promise = getPromise<string>();
            const cancelablePromise = CancelablePromise(promise);

            promise.resolve('ok');
            promise.then(() => cancelablePromise.cancel());

            return expect(cancelablePromise).resolves.toBe('ok');
        });

        test('Should be canceled', () => {
            const promise = getPromise<string>();
            const cancelablePromise = CancelablePromise(promise);

            cancelablePromise.cancel();
            cancelablePromise.cancel();
            promise.resolve('bad');

            return Promise.all([
                expect(cancelablePromise).rejects.toBe(ABORT_TOKEN),
            ]);
        });

        test('Should be resolved', () => {
            const promise = getPromise<string>();
            const cancelablePromise = CancelablePromise(promise);

            promise.resolve('ok');
            promise.then(() => cancelablePromise.cancel());

            return Promise.all([
                expect(cancelablePromise).resolves.toBe('ok'),
            ]);
        });

        test('Should be rejected', () => {
            const promise = getPromise<string>();
            const cancelablePromise = CancelablePromise(promise);

            promise.reject('error');
            promise.then(() => cancelablePromise.cancel());

            return Promise.all([
                expect(cancelablePromise).rejects.toBe('error'),
                expect(cancelablePromise.then()).rejects.toBe('error'),
            ]);
        });
    });
});
