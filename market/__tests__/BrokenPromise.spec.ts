import BrokenPromise from '../BrokenPromise';

describe('BrokenPromise', () => {
    it('должен иметь метод break', () => {
        expect(new BrokenPromise()).toHaveProperty('break');
    });

    it('должен возвращать тот же инстанс при возврате любого метода интерфейса Promise', () => {
        const promise = new BrokenPromise();

        expect(promise.then()).toBe(promise);
        expect(promise.catch()).toBe(promise);
        expect(promise.finally()).toBe(promise);
    });

    it('должен игнорировать любые методы', () => {
        const promise = new BrokenPromise();
        const cb = jest.fn();

        promise.then(cb, cb);
        promise.catch(cb);
        promise.finally(cb);
        promise.break();

        expect(cb).not.toBeCalled();
    });
});
