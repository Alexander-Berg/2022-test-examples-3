import {
    TimeUnits,
} from '../TimeUnits';
import {
    Timer,
    TimerEventName,
} from '.';

describe('Timer', () => {
    test('#.toString()', () => {
        expect(new Timer(3110, TimeUnits.Second).toString()).toEqual('51:50');
        expect(new Timer(0, TimeUnits.Minute).toString()).toEqual('00:00');
        expect(new Timer(60, TimeUnits.Minute).toString()).toEqual('00:00');
        expect(new Timer(3599, TimeUnits.Second).toString()).toEqual('59:59');
    });

    test('#.start()', () => {
        const mockRequestAnimationFrame = () => {
            let time = 0;

            return (cb: Function) => {
                time += 50;
                return cb(time);
            };
        };
        jest.spyOn(window, 'requestAnimationFrame').mockImplementation(mockRequestAnimationFrame());

        const mockOnStart = jest.fn(() => 0);
        const mockOnEnd = jest.fn(() => 1);
        const mockOnTick = jest.fn(() => 2);

        const timer = new Timer(1, TimeUnits.Second);

        timer.on(TimerEventName.Start, mockOnStart);
        timer.on(TimerEventName.End, mockOnEnd);
        timer.on(TimerEventName.Tick, mockOnTick);

        timer.start();

        expect(mockOnStart.mock.calls.length).toBe(1);
        expect(mockOnEnd.mock.calls.length).toBe(1);
        expect(mockOnTick.mock.calls.length).toBeGreaterThan(0);
    });
});
