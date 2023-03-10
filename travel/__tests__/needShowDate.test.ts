import DateMoment from '../../../interfaces/date/DateMoment';
import IThreadPlane from '../../../interfaces/state/station/IThreadPlane';
import IThreadRailroad from '../../../interfaces/state/station/IThreadRailroad';
import IStationInformerScheduleThread from '../../../interfaces/components/informer/IStationInformerScheduleThread';
import IStationInformerScheduleIntervalThread from '../../../interfaces/components/informer/IStationInformerScheduleIntervalThread';

import needShowDate from '../needShowDate';

const planeThreads: IThreadPlane[] = [
    {
        eventDt: {datetime: '2020-03-11T15:40:00+03:00' as DateMoment},
        status: {actualDt: '2020-03-11T15:40:00+03:00' as DateMoment},
    } as IThreadPlane,
    {
        eventDt: {datetime: '2020-03-12T15:40:00+03:00' as DateMoment},
        status: {actualDt: '2020-03-12T15:40:00+03:00' as DateMoment},
    } as IThreadPlane,
    {
        eventDt: {datetime: '2020-03-12T15:40:00+03:00' as DateMoment},
        status: {actualDt: '2020-03-13T15:40:00+03:00' as DateMoment},
    } as IThreadPlane,
    {
        eventDt: {datetime: '2020-03-12T15:40:00+03:00' as DateMoment},
        status: {actualDt: '2020-03-12T15:40:00+03:00' as DateMoment},
    } as IThreadPlane,
];

const railroadThreads: IThreadRailroad[] = [
    {
        departureFrom: '2020-03-11T15:40:00+03:00' as DateMoment,
        eventDt: {datetime: '2020-03-11T15:40:00+03:00' as DateMoment},
    } as IThreadRailroad,
    {
        departureFrom: '2020-03-12T15:40:00+03:00' as DateMoment,
        eventDt: {datetime: '2020-03-12T15:40:00+03:00' as DateMoment},
    } as IThreadRailroad,
    {
        departureFrom: '2020-03-12T15:40:00+03:00' as DateMoment,
        eventDt: {datetime: '2020-03-13T15:40:00+03:00' as DateMoment},
    } as IThreadRailroad,
    {
        departureFrom: '2020-03-12T15:40:00+03:00' as DateMoment,
        eventDt: {datetime: '2020-03-12T15:40:00+03:00' as DateMoment},
    } as IThreadRailroad,
];

const scheduleThreads = [
    {
        departureFrom: '2020-03-11T15:40:00+03:00',
    } as IStationInformerScheduleThread,
    {
        departureFrom: '2020-03-12T15:40:00+03:00',
    } as IStationInformerScheduleThread,
    {
        date: '2020-03-13',
    } as IStationInformerScheduleIntervalThread,
    {
        departureFrom: '2020-03-13T15:40:00+03:00',
    } as IStationInformerScheduleThread,
];

describe('needShowDate', () => {
    it('[??????????????] ???????? ?????? ???????????? ???????? ?? ???????????? - ???????????? true (???????? ???????????????????? ????????)', () => {
        expect(needShowDate(planeThreads[0], undefined)).toBe(true);
    });

    it('[??????????????] ???????? ?????? ???????????? ???????? ?? ?????????? ?????????? - ???????????? true (???????? ???????????????????? ????????)', () => {
        expect(needShowDate(planeThreads[1], planeThreads[0])).toBe(true);
    });

    it('[??????????????] ???????? ?? ?????????????????? ?? ???????????????????????? ?????????????? ???????????????????? ????????  - ???????????? true (???????? ???????????????????? ????????)', () => {
        expect(needShowDate(planeThreads[2], planeThreads[1])).toBe(true);
    });

    it('[??????????????] ???????? ???? ???????? ???? ?????????????? ???? ??????????????????????  - ???????????? false (???????? ???????????????????? ???? ????????)', () => {
        expect(needShowDate(planeThreads[3], planeThreads[2])).toBe(false);
    });

    it('[????] ???????? ?????? ???????????? ???????? ?? ???????????? - ???????????? true (???????? ???????????????????? ????????)', () => {
        expect(needShowDate(railroadThreads[0], undefined)).toBe(true);
    });

    it('[????] ???????? ?????? ???????????? ???????? ?? ?????????? ?????????? - ???????????? true (???????? ???????????????????? ????????)', () => {
        expect(needShowDate(railroadThreads[1], railroadThreads[0])).toBe(true);
    });

    it('[????] ???????? ?? ?????????????????? ?? ???????????????????????? ?????????????? ???????????????????? ????????  - ???????????? true (???????? ???????????????????? ????????)', () => {
        expect(needShowDate(railroadThreads[2], railroadThreads[1])).toBe(true);
    });

    it('[????] ???????? ???? ???????? ???? ?????????????? ???? ??????????????????????  - ???????????? false (???????? ???????????????????? ???? ????????)', () => {
        expect(needShowDate(railroadThreads[3], railroadThreads[2])).toBe(
            false,
        );
    });

    it('[bus/water] ???????? ?????? ???????????? ??????????????, ???????? ?????????????????? ?????????? (???????????? TRUE)', () => {
        expect(needShowDate(scheduleThreads[0], undefined)).toBe(true);
    });

    it('[bus/water] ???????? ?????? ???????????? ?????????????? ?? ?????????? ??????????, ???????? ?????????????????? ?????????? (???????????? TRUE)', () => {
        expect(needShowDate(scheduleThreads[1], scheduleThreads[0])).toBe(true);
    });

    it('[bus/water] ???????? ?????? ???????????? ?????????????? ?? ?????????? ?????????? ?? ???????????????? ?????????????? ????????, ???????? ?????????????????? ?????????? (???????????? TRUE)', () => {
        expect(needShowDate(scheduleThreads[2], scheduleThreads[1])).toBe(true);
    });

    it('[bus/water] ???????? ?????? ???? ???????????? ?????????????? ?? ???? ???????????? ?????????????? ?? ?????????? ??????????, ???????? ?????????????????? ???? ?????????? (???????????? FALSE)', () => {
        expect(needShowDate(scheduleThreads[3], scheduleThreads[2])).toBe(
            false,
        );
    });
});
