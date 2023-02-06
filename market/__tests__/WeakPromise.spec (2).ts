import WeakPromise from '../WeakPromise';

describe('WeakPromise', () => {
    it('должен создавать promise-like объект с идентичной Promise функциональностью', async () => {
        const resolveValue = 'resolve';
        const promise = new Promise((resolve) => setTimeout(() => resolve(resolveValue), 500));
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve(resolveValue), 500));

        expect(await weakPromise).toBe(await promise);
        expect(await weakPromise).toBe(resolveValue);
        expect(weakPromise).toHaveProperty('then');
        expect(weakPromise).toHaveProperty('catch');
        expect(weakPromise).toHaveProperty('finally');
    });

    it('должен иметь метод then, чья функциональность идентична then из Promise', async () => {
        const promise = new Promise((resolve) => setTimeout(() => resolve('resolve'), 500));
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve('resolve'), 500));

        expect(weakPromise.then((str) => `${str}/then`)).toBeInstanceOf(Promise);
        expect(weakPromise.then((str) => `${str}/then`)).toBeInstanceOf(WeakPromise);
        expect(await weakPromise.then((str) => `${str}/then`)).not.toBe(await promise);
        expect(await weakPromise.then((str) => `${str}/then`)).toBe(await promise.then((str) => `${str}/then`));
        expect(await weakPromise.then((str) => `${str}/then`)).toBe(await promise.then((str) => `${str}/then`));
    });

    it('должен иметь метод catch, чья функциональность идентична catch из Promise', async () => {
        const promise = new Promise((resolve) => setTimeout(() => resolve('resolve'), 500));
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve('resolve'), 500));

        expect(weakPromise.catch((str) => `${str}/then`)).toBeInstanceOf(Promise);
        expect(weakPromise.catch((str) => `${str}/then`)).toBeInstanceOf(WeakPromise);
        expect(await weakPromise.catch((str) => `${str}/then`)).toBe(await promise);
        expect(await weakPromise.catch((str) => `${str}/then`)).toBe(await promise.catch((str) => `${str}/then`));
    });

    it('должен иметь метод finally, чья функциональность идентична finally из Promise', async () => {
        const promise = new Promise((resolve) => setTimeout(() => resolve('resolve'), 500));
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve('resolve'), 500));

        expect(weakPromise.finally(() => 'finally')).toBeInstanceOf(Promise);
        expect(weakPromise.finally(() => 'finally')).toBeInstanceOf(WeakPromise);
        expect(await weakPromise.finally(() => 'finally')).toBe(await promise);
        expect(await weakPromise.finally(() => 'finally')).toBe(await promise.finally(() => 'finally'));
    });

    it('должен отлавливать ошибки на любых предыдущих звеньях с помощью метода catch', async () => {
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve('resolve'), 500));
        const cb = jest.fn();
        const errCb = jest.fn();

        await weakPromise
            .then(() => {
                throw new Error();
            })
            .then(cb)
            .then(cb)
            .catch(errCb);

        expect(cb).not.toBeCalled();
        expect(errCb).toBeCalled();
    });

    it('должен перехватывать ошибки с помощью метода then, если в него передан второй аргумент', async () => {
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve('resolve'), 500));
        const cb1 = jest.fn();
        const cb2 = jest.fn();
        const reject = jest.fn();
        const errCb = jest.fn();

        await weakPromise
            .then(() => {
                throw new Error();
            })
            .then(cb1, reject)
            .then(cb2)
            .catch(errCb);

        expect(cb1).not.toBeCalled();
        expect(cb2).toBeCalled();
        expect(reject).toBeCalled();
        expect(errCb).not.toBeCalled();
    });

    it('должен иметь статичный метод resolve функциональность со статичным resolve у Promise', async () => {
        expect(await WeakPromise.resolve()).toBe(await Promise.resolve());
        expect(await WeakPromise.resolve('some')).not.toBe(await Promise.resolve());
        expect(await WeakPromise.resolve('some')).toBe(await Promise.resolve('some'));
    });

    it('должен иметь статичный метод reject функциональность со статичным reject у Promise', async () => {
        await expect(WeakPromise.reject()).rejects.toBe(undefined);
        await expect(WeakPromise.reject('some')).rejects.not.toBe(undefined);
        await expect(WeakPromise.reject('some')).rejects.toBe('some');
    });

    it('должен иметь статичный метод all функциональность со статичным all у Promise', async () => {
        expect(await WeakPromise.all(['some', ''])).toEqual(await Promise.all(['some', '']));
        expect(await WeakPromise.all([Promise.resolve('some'), ''])).toEqual(
            await Promise.all([Promise.resolve('some'), '']),
        );
        expect(await WeakPromise.all([Promise.resolve('some'), Promise.resolve('other')])).toEqual(
            await Promise.all([Promise.resolve('some'), Promise.resolve('other')]),
        );
        expect(await WeakPromise.all([WeakPromise.resolve('some'), WeakPromise.resolve('other')])).toEqual(
            await Promise.all([Promise.resolve('some'), Promise.resolve('other')]),
        );
        expect(await WeakPromise.all([WeakPromise.resolve('some'), WeakPromise.resolve('other')])).toEqual(
            await Promise.all([WeakPromise.resolve('some'), WeakPromise.resolve('other')]),
        );
    });

    it('должен иметь статичный метод race функциональность со статичным race у Promise', async () => {
        expect(await WeakPromise.race(['some', ''])).toEqual(await Promise.race(['some', '']));
        expect(await WeakPromise.race([Promise.resolve('some'), ''])).toEqual(
            await Promise.race([Promise.resolve('some'), '']),
        );
        expect(await WeakPromise.race([Promise.resolve('some'), Promise.resolve('other')])).toEqual(
            await Promise.race([Promise.resolve('some'), Promise.resolve('other')]),
        );
        expect(await WeakPromise.race([WeakPromise.resolve('some'), WeakPromise.resolve('other')])).toEqual(
            await Promise.race([Promise.resolve('some'), Promise.resolve('other')]),
        );
        expect(await WeakPromise.race([WeakPromise.resolve('some'), WeakPromise.resolve('other')])).toEqual(
            await Promise.race([WeakPromise.resolve('some'), WeakPromise.resolve('other')]),
        );
    });

    it(// eslint-disable-next-line max-len
    'должен возвращать объект, соответствующий интерфейсу BreakablePromise, при вызове любого метода интерфейса Promise', () => {
        const resolveValue = 'resolve';
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve(resolveValue), 500));

        expect(weakPromise).toHaveProperty('break');
        expect(weakPromise.then()).toHaveProperty('break');
        expect(weakPromise.catch()).toHaveProperty('break');
        expect(weakPromise.finally()).toHaveProperty('break');
        expect(weakPromise.then().catch().finally().then()).toHaveProperty('break');
    });

    it('должен остановить последующее выполнение, если вызвать метод break в последовательном коде', () => {
        const resolveValue = 'resolve';
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve(resolveValue), 500));
        const cb1 = jest.fn((v) => v);
        const cb2 = jest.fn((v) => v);

        weakPromise.then(cb1).break();
        weakPromise.then(cb2);

        return new Promise<void>((r) =>
            setTimeout(() => {
                expect(cb1).toBeCalled();
                expect(cb2).toBeCalled();
                r();
            }, 1000),
        );
    });

    it('должен остановить последующее выполнение, если вызвать метод break асинхронно', async () => {
        const resolveValue = 'resolve';
        const weakPromise = new WeakPromise((resolve) => setTimeout(() => resolve(resolveValue), 500));
        const cb1 = jest.fn((v) => v);
        const cb2 = jest.fn((v) => v);

        await weakPromise.then(cb1);
        weakPromise.break();
        weakPromise.then(cb2);

        return new Promise<void>((r) =>
            setTimeout(() => {
                expect(cb1).toBeCalled();
                expect(cb2).not.toBeCalled();
                r();
            }, 1000),
        );
    });
});
