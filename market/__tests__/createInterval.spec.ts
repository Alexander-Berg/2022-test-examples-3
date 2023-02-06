/**
 * @jest-environment jsdom
 */

import createInterval from '../createInterval';

describe('createInterval', () => {
    beforeAll(() => {
        jest.useFakeTimers();
    });

    afterAll(() => {
        jest.useRealTimers();
    });

    it('should return timerId and func', () => {
        const {timerId, stop} = createInterval(jest.fn, 100);

        expect(typeof timerId).toBe('object');
        expect(typeof timerId.current).toBe('number');
        expect(typeof stop).toBe('function');
    });

    it('should run function', () => {
        const callback = jest.fn();

        createInterval(callback, 100);

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should pass args to function', () => {
        const callback = jest.fn();
        const someArgs = [
            {
                foo: 'bar',
                bar: 'foo',
            },
            {
                foo: 'bar',
                bar: 'foo',
            },
        ];

        createInterval(callback, 100, false, ...someArgs);

        expect(callback).toHaveBeenCalledWith(...someArgs);
    });

    it('should call function few times', () => {
        const callback = jest.fn();

        createInterval(callback, 100);

        jest.advanceTimersByTime(299);

        expect(callback).toHaveBeenCalledTimes(3);
    });

    it('might be stopped', () => {
        const callback = jest.fn();

        const {stop} = createInterval(callback, 100);

        jest.advanceTimersByTime(250);
        expect(callback).toHaveBeenCalledTimes(3);
        stop();
        jest.advanceTimersByTime(2500);

        expect(callback).toHaveBeenCalledTimes(3);
    });

    it('should return new timer everytime', () => {
        let savedTimerId: {current?: number} | null = null;
        const currentValues: number[] = [];

        const callback = () => {
            if (savedTimerId && savedTimerId.current) {
                currentValues.push(savedTimerId.current);
            }
        };

        const {timerId} = createInterval(callback, 100);

        savedTimerId = timerId;

        jest.advanceTimersByTime(299);

        expect(currentValues).toHaveLength(2);
        expect(currentValues[0]).not.toBe(currentValues[1]);
    });

    it('might be restartable', () => {
        const result: null[] = [];
        const callback = () => result.push(null);

        const {start, stop} = createInterval(callback, 100, true);

        expect(result).toHaveLength(0);

        start();

        jest.advanceTimersByTime(299);

        expect(result).toHaveLength(3);

        stop();

        jest.advanceTimersByTime(299);

        expect(result).toHaveLength(3);

        start();

        jest.advanceTimersByTime(299);

        expect(result).toHaveLength(6);
    });
});
