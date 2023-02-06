import moment from '../../../../reexports/moment-timezone';

import {CHAR_LIST_MARKER} from '../../stringUtils';

import getRailwayTime from '../getRailwayTime';

moment.locale('ru');

const time = '2017-06-15T18:59:00+00:00';
const data = {
    timeMoment: moment.tz(time, 'Europe/Moscow'),
    railwayMoment: moment.tz(time, 'Europe/Moscow'),
    railwayTimezone: 'Europe/Moscow',
    isAllDaysSearch: false,
};

describe('getRailwayTime', () => {
    it('should return null for unknown timezone', () => {
        expect(
            getRailwayTime({
                ...data,
                railwayMoment: moment.tz(time, 'Europe/London'),
                railwayTimezone: 'Europe/London',
            }),
        ).toEqual(null);
    });

    it('should return local time', () => {
        expect(getRailwayTime(data)).toEqual({
            time: 'местное',
            title: 'местное время',
        });
    });

    it('should return timezone with time', () => {
        expect(
            getRailwayTime({
                ...data,
                timeMoment: moment.tz(time, 'Asia/Yekaterinburg'),
            }),
        ).toEqual({
            time: 'МСК 21:59',
            title: 'Московское время',
        });
    });

    it('should return timezone with date', () => {
        expect(
            getRailwayTime({
                ...data,
                timeMoment: moment.tz(time, 'Asia/Vladivostok'),
            }),
        ).toEqual({
            time: `МСК 21:59 ${CHAR_LIST_MARKER} 15 июня`,
            title: 'Московское время',
        });
    });
});
