import DateMoment from '../../../interfaces/date/DateMoment';

import getScheduleThreadsAsTable from '../getScheduleThreadsAsTable';

const thread_0_00 = {
    canonicalUid: '0:00',
    departureFrom: '2020-08-25T00:00:00+03:00' as DateMoment,
};
const thread_0_10 = {
    canonicalUid: '0:10',
    departureFrom: '2020-08-25T00:10:00+03:00' as DateMoment,
};
const thread_1_00 = {
    canonicalUid: '1:00',
    departureFrom: '2020-08-25T01:00:00+03:00' as DateMoment,
};
const thread_1_59 = {
    canonicalUid: '1:59',
    departureFrom: '2020-08-25T01:59:00+03:00' as DateMoment,
};
const thread_2_00 = {
    canonicalUid: '2:00',
    departureFrom: '2020-08-25T02:00:00+03:00' as DateMoment,
};
const thread_3_00 = {
    canonicalUid: '3:00',
    departureFrom: '2020-08-25T03:00:00+03:00' as DateMoment,
};
const thread_9_00 = {
    canonicalUid: '9:00',
    departureFrom: '2020-08-25T09:00:00+03:00' as DateMoment,
};
const thread_9_10 = {
    canonicalUid: '9:10',
    departureFrom: '2020-08-25T09:10:00+03:00' as DateMoment,
};
const thread_9_20 = {
    canonicalUid: '9:20',
    departureFrom: '2020-08-25T09:20:00+03:00' as DateMoment,
};
const thread_9_20_2 = {
    canonicalUid: '9:20:20',
    departureFrom: '2020-08-25T09:20:20+03:00' as DateMoment,
};
const thread_10_00 = {
    canonicalUid: '10:00',
    departureFrom: '2020-08-25T10:00:00+03:00' as DateMoment,
};
const thread_23_00 = {
    canonicalUid: '23:00',
    departureFrom: '2020-08-25T23:00:00+03:00' as DateMoment,
};
const thread_23_59 = {
    canonicalUid: '23:59',
    departureFrom: '2020-08-25T23:59:00+03:00' as DateMoment,
};

describe('getScheduleThreadsAsTable', () => {
    it('Для пустого массива вернет пустой массив', () => {
        expect(getScheduleThreadsAsTable([])).toEqual([]);
    });

    it('Разбиение по колонкам в один час', () => {
        expect(
            getScheduleThreadsAsTable([
                thread_0_00,
                thread_0_10,
                thread_1_00,
                thread_1_59,
                thread_2_00,
                thread_3_00,
                thread_9_00,
                thread_9_10,
                thread_9_20,
                thread_9_20_2,
                thread_10_00,
                thread_23_00,
                thread_23_59,
            ]),
        ).toStrictEqual([
            {threads: [thread_0_00, thread_0_10], interval: '0'},
            {threads: [thread_1_00, thread_1_59], interval: '1'},
            {threads: [thread_2_00], interval: '2'},
            {threads: [thread_3_00], interval: '3'},
            {
                threads: [thread_9_00, thread_9_10, thread_9_20, thread_9_20_2],
                interval: '9',
            },
            {threads: [thread_10_00], interval: '10'},
            {threads: [thread_23_00, thread_23_59], interval: '23'},
        ]);
    });

    it('Разбиение по колонкам в два часа', () => {
        expect(
            getScheduleThreadsAsTable(
                [
                    thread_0_00,
                    thread_0_10,
                    thread_1_00,
                    thread_1_59,
                    thread_2_00,
                    thread_3_00,
                    thread_9_00,
                    thread_9_10,
                    thread_9_20,
                    thread_9_20_2,
                    thread_10_00,
                    thread_23_00,
                    thread_23_59,
                ],
                6,
            ),
        ).toStrictEqual([
            {
                threads: [thread_0_00, thread_0_10, thread_1_00, thread_1_59],
                interval: '0-1',
            },
            {threads: [thread_2_00, thread_3_00], interval: '2-3'},
            {
                threads: [thread_9_00, thread_9_10, thread_9_20, thread_9_20_2],
                interval: '8-9',
            },
            {threads: [thread_10_00], interval: '10-11'},
            {threads: [thread_23_00, thread_23_59], interval: '22-23'},
        ]);
    });
});
