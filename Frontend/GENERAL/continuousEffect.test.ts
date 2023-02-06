import { createContinuousEffect, ContinuousEffect } from './continuousEffect';

// const effect = () => createContinuousEffect(async function*(x: string = 'ok') {
//     if (x === 'throw1') { throw new Error('Throw at the first') }
//     yield `1 ${x}`;
//     if (x === 'throw2') { throw new Error('Throw at the second') }
//     yield `2 ${x}`;
//     if (x === 'throw3') { throw new Error('Throw at the third') }
//     return true;
// });

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type CEffect = ContinuousEffect<any, any>;
const spyEvent = (effect: CEffect, name: keyof CEffect) => {
    const fn = jest.fn();
    effect[name].watch(fn);
    return fn;
};

describe('continuousEffect', () => {
    it('should works as effect by default', async() => {
        expect.assertions(6);

        const fx = createContinuousEffect(async(x: string) => x === 'ok');
        const done = spyEvent(fx, 'done');
        const doneData = spyEvent(fx, 'doneData');

        const resultOk = await fx('ok');

        expect(resultOk).toEqual(true);
        expect(done).toBeCalledWith({ params: 'ok', result: true });
        expect(doneData).toBeCalledWith(true);

        const resultNok = await fx('nok');

        expect(done).toBeCalledWith({ params: 'nok', result: false });
        expect(doneData).toBeCalledWith(false);
        expect(resultNok).toEqual(false);
    });

    it('should call progress and progressData on each yield in handler', async() => {
        expect.assertions(6);

        const fx = createContinuousEffect<void, string, number>(async function*() { yield 1; yield 2; return 'result' });
        const progress = spyEvent(fx, 'progress');
        const progressData = spyEvent(fx, 'progressData');

        const resultOk = await fx();

        expect(progress).toBeCalledWith({ params: undefined, result: 1 });
        expect(progress).toBeCalledWith({ params: undefined, result: 2 });
        expect(progress).toBeCalledTimes(2);
        expect(progressData).toBeCalledWith(1);
        expect(progressData).toBeCalledWith(2);
        expect(resultOk).toEqual('result');
    });

    it('should allow to throw after yield', async() => {
        expect.assertions(3);

        const fx = createContinuousEffect(async function*(v: string) {
            yield v;
            throw new Error('err');
        });
        const progress = spyEvent(fx, 'progress');

        await expect(fx('x')).rejects.toMatchObject({ message: 'err' });

        expect(progress).toBeCalledWith({ params: 'x', result: 'x' });
        expect(progress).toBeCalledTimes(1);
    });
});
