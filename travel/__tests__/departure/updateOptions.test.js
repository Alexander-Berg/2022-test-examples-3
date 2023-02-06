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
        endTime: '20:59:59',
    },
};

describe('departure.updateOptions', () => {
    it('Обновит опции фильтра с использованием обычного сегмента', () => {
        const options = departure.updateOptions(
            departure.getDefaultOptions(),
            segment,
        );

        expect(options.length).toBe(1);
        expect(options).toEqual(expect.arrayContaining([TimeOfDay.night]));
    });

    it('Обновит опции фильтра с использованием интервального сегмента', () => {
        const options = departure.updateOptions(
            departure.getDefaultOptions(),
            intervalSegment,
        );

        expect(options.length).toBe(3);
        expect(options).toEqual(
            expect.arrayContaining([
                TimeOfDay.day,
                TimeOfDay.evening,
                TimeOfDay.morning,
            ]),
        );
    });
});
