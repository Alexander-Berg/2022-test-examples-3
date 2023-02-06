import { checkTimeIntervalCondition } from '../../../components/helpers/time';
import { ONE_DAY } from '../../../components/consts';

describe('timeHelper -->', () => {
    const timeout = 5;
    const setCurrentDate = (ms) => {
        Date.now = jest.fn(() => ms);
    };

    describe('checkTimeIntervalCondition', () => {
        it('возвращает true, если указанное время равно 0 мс', () => {
            setCurrentDate(20);
            expect(checkTimeIntervalCondition(0, 0)).toBe(true);
        });

        it('возвращает true, если разница между текущим временем (в мс) и указанным временем больше таймаута', () => {
            setCurrentDate(20);
            expect(checkTimeIntervalCondition(10, timeout)).toBe(true);
            expect(checkTimeIntervalCondition(18, timeout)).toBe(false);
        });

        it('возвращает true, если разница между текущим временем (в виде объекта Date) и указанным временем больше таймаута', () => {
            setCurrentDate(new Date('2000-01-02 01:00:00').getTime());
            expect(checkTimeIntervalCondition(new Date('2000-01-01 00:00:00'), ONE_DAY)).toBe(true);
            expect(checkTimeIntervalCondition(new Date('2000-01-01 02:00:00'), ONE_DAY)).toBe(false);
        });

        it('возвращает false, если разница между текущим временем (в мс) и указанным временем больше таймаута, и передан признак isLesser = true', () => {
            setCurrentDate(20);
            expect(checkTimeIntervalCondition(10, timeout, null, true)).toBe(false);
            expect(checkTimeIntervalCondition(18, timeout, null, true)).toBe(true);
        });

        it('возвращает true, если разница между текущим временем (в виде объекта Date) и указанным временем больше таймаута, и передан признак isLesser = true', () => {
            setCurrentDate(new Date('2000-01-02 01:00:00').getTime());
            expect(checkTimeIntervalCondition(new Date('2000-01-01 00:00:00'), ONE_DAY, null, true)).toBe(false);
            expect(checkTimeIntervalCondition(new Date('2000-01-01 02:00:00'), ONE_DAY, null, true)).toBe(true);
        });

        it('возвращает true, если указанное время находится в будущем, и передан признак isLesser = true', () => {
            setCurrentDate(20);
            expect(checkTimeIntervalCondition(100, timeout, null, true)).toBe(true);
            expect(checkTimeIntervalCondition(100, timeout)).toBe(false);
        });
        it('если передан параметр серверного времени, исчисляет относительно него', () => {
            setCurrentDate(20);
            expect(checkTimeIntervalCondition(0, 0, 1619450502001)).toBe(true);
        });
    });
});
