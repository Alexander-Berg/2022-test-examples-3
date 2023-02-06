const makeCancelable = require('../lib/make-cancelable-promise');
const expect = require('expect');

describe('make-cancelable-promise', () => {
    it('промис-обертка промиса, который должен зарезолвится, реджектится c параметром isCanceled, если его отменить', (done) => {
        const promise = new Promise((resolve) => {
            resolve('success');
        });

        const wrapped = makeCancelable(promise);
        wrapped.cancel();

        const callback = jest.fn();
        const fallback = jest.fn();
        wrapped.promise
            .then(callback)
            .catch(fallback)
            .then(() => {
                expect(callback.mock.calls.length).toEqual(0);
                expect(fallback.mock.calls.length).toEqual(1);
                expect(fallback.mock.calls[0][0]).toEqual({ isCanceled: true });
                done();
            });
    });

    it('промис-обертка промиса, который должен зареджектиться, реджектится с параметром isCanceled, если его отменить', (done) => {
        // @ts-ignore
        const promise = new Promise((resolve, reject) => {
            reject('error');
        });

        const wrapped = makeCancelable(promise);
        wrapped.cancel();

        const callback = jest.fn();
        const fallback = jest.fn();
        wrapped.promise
            .then(callback)
            .catch(fallback)
            .then(() => {
                expect(callback.mock.calls.length).toEqual(0);
                expect(fallback.mock.calls.length).toEqual(1);
                expect(fallback.mock.calls[0][0]).toEqual({ isCanceled: true });
                done();
            });
    });

    it('промис-обертка, резолвится с данными промиса, если его не отменять', (done) => {
        const promise = new Promise((resolve) => {
            resolve('success');
        });

        const wrapped = makeCancelable(promise);

        const callback = jest.fn();
        const fallback = jest.fn();
        wrapped.promise
            .then(callback)
            .catch(fallback)
            .then(() => {
                expect(callback.mock.calls.length).toEqual(1);
                expect(fallback.mock.calls.length).toEqual(0);
                expect(callback.mock.calls[0][0]).toEqual('success');
                done();
            });
    });

    it('промис-обертка реджектится с данными промиса, если его не отменять', (done) => {
        // @ts-ignore
        const promise = new Promise((resolve, reject) => {
            reject('error');
        });

        const wrapped = makeCancelable(promise);

        const callback = jest.fn();
        const fallback = jest.fn();
        wrapped.promise
            .then(callback)
            .catch(fallback)
            .then(() => {
                expect(callback.mock.calls.length).toEqual(0);
                expect(fallback.mock.calls.length).toEqual(1);
                expect(fallback.mock.calls[0][0]).toEqual('error');
                done();
            });
    });
});
