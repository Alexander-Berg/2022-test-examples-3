import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import departure from '../../departure';

const segment = {
    departure: '2016-08-06T00:00:00+00:00',
    timezoneFrom: 'Asia/Yekaterinburg',
};
const intervalSegment = {
    isInterval: true,
    thread: {
        beginTime: '06:00:00',
        endTime: '17:59:59',
    },
};

describe('departure.apply', () => {
    it('Вернёт true для дефолтного значения', () => {
        expect(departure.apply(departure.getDefaultValue(), segment)).toBe(
            true,
        );
    });

    it('Вернёт true если время прибытия соответствует заданному значению', () => {
        expect(departure.apply([TimeOfDay.night, TimeOfDay.day], segment)).toBe(
            true,
        );
    });

    it('Вернёт false если время прибытия не соответствует заданному значению', () => {
        expect(departure.apply([TimeOfDay.day], segment)).toBe(false);
    });

    it('Вернёт true для интервального сегмента, если время хождения соответствует заданному значению', () => {
        expect(departure.apply([TimeOfDay.day], intervalSegment)).toBe(true);
    });

    it('Вернёт false для интервального сегмента, если время хождения не соответствует заданному значению', () => {
        expect(departure.apply([TimeOfDay.evening], intervalSegment)).toBe(
            false,
        );
    });
});
