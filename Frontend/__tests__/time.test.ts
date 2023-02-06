import { getTime, TimeUnit } from '../time';

describe('#getTime', () => {
    const data = [
        [5000, 'ms', 's', 5],
        [500000, 'us', 's', 0.5],
        [3, 'ms', 'us', 3000],
        [3, 's', 'us', 3000000],
    ] as const;

    test.each(data)('%i from %s to %s', (timestamp: number, from: TimeUnit, to: TimeUnit, exp: number) => {
        expect(getTime(timestamp, from, to)).toBe(exp);
    });
});
