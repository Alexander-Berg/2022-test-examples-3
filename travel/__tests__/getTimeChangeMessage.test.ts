import moment from 'moment';

import {TIME} from '../../../../lib/date/formats';

import ThreadStatus from '../../../../interfaces/state/station/ThreadStatus';
import StationEventList from '../../../../interfaces/state/station/StationEventList';
import IStationThreadStatus from '../../../../interfaces/state/station/IStationThreadStatus';
import DateMoment from '../../../../interfaces/date/DateMoment';
import Lang from '../../../../interfaces/Lang';

import getTimeChangeMessage from '../getTimeChangeMessage';
import deleteLastSymbol from '../../../../lib/string/deleteLastSymbol';

moment.locale(Lang.ru);

const actualDt = '2020-04-06T00:05:00+03:00' as DateMoment;
const actualTime = moment.parseZone(actualDt).format(TIME);
const actualDate = deleteLastSymbol(
    moment.parseZone(actualDt).format('D MMM'),
    '.',
);
const expectedDt = '2020-04-06T00:04:00+03:00' as DateMoment;
const expectedTime = moment.parseZone(expectedDt).format(TIME);

const eventDt = {
    time: expectedTime,
    datetime: expectedDt,
};
const delayedStatus: IStationThreadStatus = {
    status: ThreadStatus.delayed,
    actualDt,
};
const arrivalDelayedExpectedMessage = `Совершит посадку в ${actualTime}`;
const departureDelayedExpectedMessage = `Вылет задержан до ${actualTime}`;

const arrivedStatus: IStationThreadStatus = {
    status: ThreadStatus.arrived,
    actualDt,
};
const arrivalArrivedExpectedMessage = `Совершил посадку в ${actualTime}`;

const canceledStatus: IStationThreadStatus = {
    status: ThreadStatus.cancelled,
};
const canceledExpectedMessage = 'Отменен';

const departedStatus: IStationThreadStatus = {
    status: ThreadStatus.departed,
    diverted: {
        settlement: 'Челябинск',
        iataCode: 'CEK',
        title: 'Баландино',
    },
};
const departedExpectedMessage = 'Направлен в аэропорт Баландино';

const earlyStatus: IStationThreadStatus = {
    status: ThreadStatus.early,
    actualDt,
};
const earlyExpectedMessage = `Вылет в ${actualTime}`;

const onTimeStatus: IStationThreadStatus = {
    status: ThreadStatus.onTime,
};

const delayedStatusWithoutActualDt: IStationThreadStatus = {
    status: ThreadStatus.delayed,
};
const depatrureDelayedWithoutActualDtExpectedMessage = 'Вылет задержан';

const arrivedStatusWithoutActualDt: IStationThreadStatus = {
    status: ThreadStatus.arrived,
};
const arrivalArrivedWithoutActualDtExpectedMessage = 'Прилетел';

const earlyStatusWithoutActualDt: IStationThreadStatus = {
    status: ThreadStatus.early,
};
const departureEarlydWithoutActualDtExpectedMessage = 'Вылет раньше';

describe('getTimeChangeMessage', () => {
    it('Прилет ожидается раньше', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                {
                    status: ThreadStatus.early,
                    actualDt,
                },
                StationEventList.arrival,
            ),
        ).toBe(`Совершит посадку в ${actualTime}`);

        expect(
            getTimeChangeMessage(
                eventDt,
                {
                    status: ThreadStatus.early,
                },
                StationEventList.arrival,
            ),
        ).toBe('');
    });

    it('Вернет строку "Совершит посадку в 02:05", когда event === arrival, status === delayed', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                delayedStatus,
                StationEventList.arrival,
            ),
        ).toBe(arrivalDelayedExpectedMessage);
    });

    it('Вернет пустую строку, когда event === arrival, status === delayed, isMobile === true', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                delayedStatus,
                StationEventList.arrival,
                true,
            ),
        ).toBe('');
    });

    it('Вернет строку "Вылет задержан до 02:05", когда event === departure, status === delayed', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                delayedStatus,
                StationEventList.departure,
            ),
        ).toBe(departureDelayedExpectedMessage);
    });

    it('Вернет строку "Совершил посадку в 02:05", когда event === arrival, status === arrived', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                arrivedStatus,
                StationEventList.arrival,
            ),
        ).toBe(arrivalArrivedExpectedMessage);
    });

    it('Вернет пустую строку, когда event === arrival, status === arrived, isMobile === true', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                arrivedStatus,
                StationEventList.arrival,
                true,
            ),
        ).toBe('');
    });

    it('Вернет пустую строку, когда event === departure, status === arrived', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                arrivedStatus,
                StationEventList.departure,
            ),
        ).toBe('');
    });

    it('Вернет строку "Отменен", когда event === arrival, status === canceled', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                canceledStatus,
                StationEventList.arrival,
            ),
        ).toBe(canceledExpectedMessage);
    });

    it('Вернет строку "Отменен", когда event === arrival, status === canceled, isMobile === true', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                canceledStatus,
                StationEventList.arrival,
                true,
            ),
        ).toBe(canceledExpectedMessage);
    });

    it('Вернет строку "Отменен", когда event === departure, status === canceled', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                canceledStatus,
                StationEventList.departure,
            ),
        ).toBe(canceledExpectedMessage);
    });

    it('Вернет строку "Направлен в аэропорт Баландино", когда event === arrival, присутствует поле departed', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                departedStatus,
                StationEventList.arrival,
            ),
        ).toBe(departedExpectedMessage);
    });

    it('Вернет строку "Направлен в аэропорт Баландино", когда event === arrival, присутствует поле departed, isMobile === true', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                departedStatus,
                StationEventList.arrival,
                true,
            ),
        ).toBe(departedExpectedMessage);
    });

    it('Вернет строку "Направлен в аэропорт Баландино", когда event === departure, присутствует поле departed', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                departedStatus,
                StationEventList.departure,
            ),
        ).toBe(departedExpectedMessage);
    });

    it('Вернет строку "Вылет в 02:05", когда event === departure, status === early', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                earlyStatus,
                StationEventList.departure,
            ),
        ).toBe(earlyExpectedMessage);
    });

    it('Вернет пустую строку, когда event === departure, status === early, isMobile === true', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                earlyStatus,
                StationEventList.departure,
                true,
            ),
        ).toBe('');
    });

    it('Вернет пустую строку, когда event === arrival, status === on-time', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                onTimeStatus,
                StationEventList.arrival,
            ),
        ).toBe('');
    });

    it('Вернет пустую строку, когда event === departure, status === on-time', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                onTimeStatus,
                StationEventList.departure,
            ),
        ).toBe('');
    });

    it('Вернет строку "Вылет задержан", когда event === departure, status === delayed, не передано актуальное время', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                delayedStatusWithoutActualDt,
                StationEventList.departure,
            ),
        ).toBe(depatrureDelayedWithoutActualDtExpectedMessage);
    });

    it('Вернет строку "Прилетел", когда event === arrival, status === arrived, не передано актуальное время', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                arrivedStatusWithoutActualDt,
                StationEventList.arrival,
            ),
        ).toBe(arrivalArrivedWithoutActualDtExpectedMessage);
    });

    it('Вернет строку "Вылет раньше", когда event === departure, status === early, не передано актуальное время', () => {
        expect(
            getTimeChangeMessage(
                eventDt,
                earlyStatusWithoutActualDt,
                StationEventList.departure,
            ),
        ).toBe(departureEarlydWithoutActualDtExpectedMessage);
    });

    it('В случае, если ожидаемая дата вылета/прилета и актуальная отличаются, то вернет строку с актуальным временем и датой', () => {
        expect(
            getTimeChangeMessage(
                {
                    time: '04:00',
                    datetime: '2020-04-05T00:04:00+03:00' as DateMoment,
                },
                delayedStatus,
                StationEventList.departure,
            ),
        ).toBe(`Вылет задержан до ${actualTime} ${actualDate}`);
    });
});
