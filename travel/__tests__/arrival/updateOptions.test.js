import TimeOfDay from '../../../../interfaces/date/TimeOfDay';

import arrival from '../../arrival';

const segment = {
    arrival: '2016-08-06T00:00:00+00:00',
    timezoneTo: 'Asia/Yekaterinburg',
};
const intervalSegment = {
    isInterval: true,
    thread: {
        beginTime: '06:00:00',
        endTime: '20:59:59',
    },
};

describe('arrival.updateOptions', () => {
    it('Обновит опции фильтра с использованием обычного сегмента', () => {
        const options = arrival.updateOptions(
            arrival.getDefaultOptions(),
            segment,
        );

        expect(options.length).toBe(1);
        expect(options).toEqual(expect.arrayContaining([TimeOfDay.night]));
    });

    it('Обновит опции фильтра с использованием интервального сегмента', () => {
        const options = arrival.updateOptions(
            arrival.getDefaultOptions(),
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
