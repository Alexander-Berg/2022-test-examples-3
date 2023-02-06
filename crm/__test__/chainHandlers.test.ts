import { chainHandlers } from '../chainHandlers';

describe('chainHandlers', () => {
    it('calls all fn', async () => {
        const ctx = {};

        const fn1 = jest.fn();
        const fn2 = jest.fn();
        await chainHandlers(fn1, fn2)(ctx);

        expect(fn1).toBeCalledWith(ctx);
        expect(fn2).toBeCalledWith(ctx);
    });

    it('calls fns until reject', async () => {
        const fn1 = jest.fn(() => Promise.reject('error'));
        const fn2 = jest.fn();

        try {
            await chainHandlers(fn1, fn2)({});
        } catch (error) {
            expect(error).toBe('error');
        }

        expect(fn1).toBeCalled();
        expect(fn2).not.toBeCalled();
    });
});
