import TDateRobot from 'types/common/date/TDateRobot';

import getIntervalsForRequestTableDynamic from 'projects/avia/lib/dynamic/getIntervalsForRequestTableDynamic';

describe('getIntervalsForRequestTableDynamic', () => {
    it('Должны вернуться диапазоны для всех диагоналей для разных смещений таблицы относительно основной даты', () => {
        const expectData = [
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-24' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-23' as TDateRobot,
            },
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-25' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-23' as TDateRobot,
            },
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-26' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-22' as TDateRobot,
            },
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-27' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-21' as TDateRobot,
            },
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-28' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-20' as TDateRobot,
            },
            {
                when: '2021-05-21' as TDateRobot,
                returnDate: '2021-05-24' as TDateRobot,
                startDate: '2021-05-21' as TDateRobot,
                endDate: '2021-05-23' as TDateRobot,
            },
            {
                when: '2021-05-22' as TDateRobot,
                returnDate: '2021-05-24' as TDateRobot,
                startDate: '2021-05-22' as TDateRobot,
                endDate: '2021-05-23' as TDateRobot,
            },
            {
                when: '2021-05-23' as TDateRobot,
                returnDate: '2021-05-24' as TDateRobot,
                startDate: '2021-05-23' as TDateRobot,
                endDate: '2021-05-23' as TDateRobot,
            },
        ];

        expect(
            getIntervalsForRequestTableDynamic({
                forwardDate: '2021-05-20' as TDateRobot,
                backwardDate: '2021-05-24' as TDateRobot,
                leftRange: 0,
                rightRange: 4,
                topRange: 0,
                bottomRange: 3,
            }),
        ).toEqual(expectData);

        expect(
            getIntervalsForRequestTableDynamic({
                forwardDate: '2021-05-20' as TDateRobot,
                backwardDate: '2021-05-25' as TDateRobot,
                leftRange: 1,
                rightRange: 3,
                topRange: 0,
                bottomRange: 3,
            }),
        ).toEqual(expectData);

        expect(
            getIntervalsForRequestTableDynamic({
                forwardDate: '2021-05-21' as TDateRobot,
                backwardDate: '2021-05-25' as TDateRobot,
                leftRange: 1,
                rightRange: 3,
                topRange: 1,
                bottomRange: 2,
            }),
        ).toEqual(expectData);
    });

    it('Фильтрация несуществующих вариантов путешествий (например дата "туда" после "обратно")', () => {
        expect(
            getIntervalsForRequestTableDynamic({
                forwardDate: '2021-05-20' as TDateRobot,
                backwardDate: '2021-05-20' as TDateRobot,
                leftRange: 0,
                rightRange: 1,
                topRange: 0,
                bottomRange: 1,
            }),
        ).toEqual([
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-20' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-21' as TDateRobot,
            },
            {
                when: '2021-05-20' as TDateRobot,
                returnDate: '2021-05-21' as TDateRobot,
                startDate: '2021-05-20' as TDateRobot,
                endDate: '2021-05-20' as TDateRobot,
            },
        ]);
    });
});
