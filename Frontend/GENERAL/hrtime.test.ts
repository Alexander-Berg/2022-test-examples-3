import { hrtimeHumanize } from './hrtime';

describe('hrtime util', function () {
    it('hrtimeHumanize returns expected value', () => {
        const timeEnd: [number, number] = [5, 123];

        const time = hrtimeHumanize(timeEnd);

        expect(time.seconds).toBe(5.000000123);
        expect(time.milliseconds).toBe(5000.000123);
        expect(time.nanoseconds).toBe(5000000123);
    });
});
