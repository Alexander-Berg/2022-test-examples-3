import getThrottle from '../getThrottle';

jest.useFakeTimers();

const fakeFunction = jest.fn();
const throttledFunction = getThrottle(fakeFunction, 200);

describe('getThrottle', () => {
    beforeEach(() => {
        jest.clearAllTimers();
    });

    it('Целевая функция вызывается только один раз, если между вызовами обертки не прошел таймаут', () => {
        expect(fakeFunction.mock.calls.length).toBe(0);
        throttledFunction(1);
        throttledFunction(2);
        expect(fakeFunction.mock.calls.length).toBe(0);
        jest.advanceTimersByTime(300);
        expect(fakeFunction.mock.calls.length).toBe(1);
        expect(fakeFunction.mock.calls).toEqual([[2]]);
        throttledFunction(3);
        jest.advanceTimersByTime(300);
        expect(fakeFunction.mock.calls.length).toBe(2);
        expect(fakeFunction.mock.calls).toEqual([[2], [3]]);
    });

    it('Вызов метода flush приведет к вызову целевой функции', () => {
        expect(fakeFunction.mock.calls.length).toBe(0);
        throttledFunction(1);
        expect(fakeFunction.mock.calls.length).toBe(0);
        jest.advanceTimersByTime(100);
        expect(fakeFunction.mock.calls.length).toBe(0);
        throttledFunction.flush();
        expect(fakeFunction.mock.calls.length).toBe(1);
        expect(fakeFunction.mock.calls).toEqual([[1]]);
        throttledFunction.flush();
        expect(fakeFunction.mock.calls.length).toBe(1);
        expect(fakeFunction.mock.calls).toEqual([[1]]);
        jest.advanceTimersByTime(300);
        expect(fakeFunction.mock.calls.length).toBe(1);
        expect(fakeFunction.mock.calls).toEqual([[1]]);
    });
});
