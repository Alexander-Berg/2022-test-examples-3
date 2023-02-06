import moment from '../../../../reexports/moment-timezone';

import ISegment from '../../../interfaces/segment/ISegment';

import {getLocalTime} from '../cachedSegmentTime';

jest.mock('../../../../reexports/moment-timezone', () => ({
    tz: jest.fn((time, timezone) => `${time}-${timezone}`),
}));

describe('cachedSegmentTime', () => {
    it('getLocalTime', () => {
        const departure = '2019-10-13T03:30:00+00:00';
        const arrival = '2019-10-13T04:47:00+00:00';
        const segment = {
            departure,
            arrival,
        } as ISegment;
        const timezoneFirst = 'Asia/Yekaterinburg';
        const timezoneSecond = 'Europe/Moscow';

        const result1 = getLocalTime(segment, 'departure', timezoneFirst);
        const result2 = getLocalTime(segment, 'departure', timezoneFirst);
        const result3 = getLocalTime(segment, 'departure', timezoneSecond);
        const result4 = getLocalTime(segment, 'arrival', timezoneFirst);
        const result5 = getLocalTime(segment, 'arrival', timezoneFirst);

        expect(result1).toBe(`${departure}-${timezoneFirst}`);
        expect(result2).toBe(`${departure}-${timezoneFirst}`);
        expect(result3).toBe(`${departure}-${timezoneSecond}`);
        expect(result4).toBe(`${arrival}-${timezoneFirst}`);
        expect(result5).toBe(`${arrival}-${timezoneFirst}`);
        expect((moment.tz as unknown as jest.Mock).mock.calls).toEqual([
            [departure, timezoneFirst],
            [departure, timezoneSecond],
            [arrival, timezoneFirst],
        ]);
    });
});
