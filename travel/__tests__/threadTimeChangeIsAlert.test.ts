import StationEventList from '../../../../interfaces/state/station/StationEventList';
import ThreadStatus from '../../../../interfaces/state/station/ThreadStatus';
import IRouteStation from '../../../../interfaces/state/station/IRouteStation';

import threadTimeChangeIsAlert from '../threadTimeChangeIsAlert';

describe('threadTimeChangeIsAlert', () => {
    it('Если нет информации о рейсе вернет false', () => {
        expect(
            threadTimeChangeIsAlert(StationEventList.departure, null, null),
        ).toBe(false);
    });

    it('Рейс отменен - is alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.departure,
                {
                    status: ThreadStatus.cancelled,
                },
                null,
            ),
        ).toBe(true);
    });

    it('Рейс направлен в другой аэропорт - is alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.departure,
                {
                    status: ThreadStatus.onTime,
                    diverted: {} as IRouteStation,
                },
                null,
            ),
        ).toBe(true);
    });

    it('Прилет/вылет рейса ожидается позже запланированного на 4 мин - is no alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.departure,
                {
                    status: ThreadStatus.delayed,
                },
                -4,
            ),
        ).toBe(false);

        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.delayed,
                },
                -4,
            ),
        ).toBe(false);
    });

    it('Прилет/вылет рейса ожидается позже запланированного на 5 мин - is alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.departure,
                {
                    status: ThreadStatus.delayed,
                },
                -5,
            ),
        ).toBe(true);

        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.delayed,
                },
                -5,
            ),
        ).toBe(true);
    });

    it('Прилет рейса ожидается раньше запланированного - is no alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.early,
                },
                -5,
            ),
        ).toBe(false);

        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.early,
                },
                -30,
            ),
        ).toBe(false);
    });

    it('Самолет прилетел позже запланированного на 4 мин - is no alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.arrived,
                },
                -4,
            ),
        ).toBe(false);
    });

    it('Самолет прилетел позже запланированного на 5 мин - is alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.arrived,
                },
                -5,
            ),
        ).toBe(true);
    });

    it('Самолет прилетел раньше запланированного - is no alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.arrived,
                },
                2,
            ),
        ).toBe(false);

        expect(
            threadTimeChangeIsAlert(
                StationEventList.arrival,
                {
                    status: ThreadStatus.arrived,
                },
                40,
            ),
        ).toBe(false);
    });

    it('Самолет вылетает раньше на 29 мин - is no alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.departure,
                {
                    status: ThreadStatus.early,
                },
                29,
            ),
        ).toBe(false);
    });

    it('Самолет вылетает раньше на 30 мин - is alert', () => {
        expect(
            threadTimeChangeIsAlert(
                StationEventList.departure,
                {
                    status: ThreadStatus.early,
                },
                30,
            ),
        ).toBe(true);
    });
});
