import {CHAR_MIDDLE_DASH} from '../../../../lib/stringUtils';

import ThreadStatus from '../../../../interfaces/state/station/ThreadStatus';
import StationEventList from '../../../../interfaces/state/station/StationEventList';
import IStationThreadStatus from '../../../../interfaces/state/station/IStationThreadStatus';
import AdditionalThreadStatus from '../../../../interfaces/state/station/AdditionalThreadStatus';

import getMessageFromStatus from '../getMessageFromStatus';

const terminal = 'B';
const hoursBeforeEvent = 10;

const unknownStatus: IStationThreadStatus = {
    status: ThreadStatus.unknown,
};

const unknownExpectMessage = [
    {text: 'Нет актуальной информации о статусе', status: ThreadStatus.unknown},
];

const canceledStatus: IStationThreadStatus = {
    status: ThreadStatus.cancelled,
};

const onTimeStatus: IStationThreadStatus = {
    status: ThreadStatus.onTime,
};
const onTimeArrivalExpectedMessages = {text: 'Прилетит по расписанию'};

const earlyCheckInDesksStatus: IStationThreadStatus = {
    status: ThreadStatus.early,
    checkInDesks: '200-202',
};
const earlyCheckInDesksDepartureExpectedMessages = [
    {text: 'Вылет раньше'},
    {text: `Стойки регистрации 200${CHAR_MIDDLE_DASH}202`},
];

const earlyCheckInDesksGateStatus: IStationThreadStatus = {
    status: ThreadStatus.early,
    checkInDesks: '200-202',
    gate: '15',
};

const earlyCheckInDesksGateDepartureExpectedMessages = [
    {text: 'Вылет раньше'},
    {text: `Стойки регистрации 200${CHAR_MIDDLE_DASH}202`},
    {text: 'Выход на посадку 15'},
];

const departedDivertedStatus: IStationThreadStatus = {
    status: ThreadStatus.departed,
    diverted: {
        settlement: 'Челябинск',
        iataCode: 'CEK',
        title: 'Баландино',
    },
};
const departedDivertedDepartureExpectedMessages = [{text: 'Вылетел'}];

const delayedStatus: IStationThreadStatus = {
    status: ThreadStatus.delayed,
};
const delayedArrivalExpectedMessages = [{text: 'Прилетит с опозданием'}];

const arrivedBaggageCarouselsStatus: IStationThreadStatus = {
    status: ThreadStatus.arrived,
    baggageCarousels: '5',
};
const arrivalArrivedBaggageCarouselExpectedMessages = [
    {text: 'Прилетел'},
    {text: 'Выдача багажа: лента 5'},
];

const arrivedBaggageCarouselsGateStatus: IStationThreadStatus = {
    status: ThreadStatus.arrived,
    baggageCarousels: '5',
    gate: '15',
};
const arrivalArrivedBaggageCarouselGateExpectedMessages = [
    {text: 'Прилетел'},
    {text: 'Выход на высадку 15'},
    {text: 'Выдача багажа: лента 5'},
];

describe('getMessageFromStatus', () => {
    it('Прилет ожмдается раньше', () => {
        expect(
            getMessageFromStatus(
                {
                    status: ThreadStatus.early,
                },
                StationEventList.arrival,
                hoursBeforeEvent,
            ),
        ).toStrictEqual([{text: 'Прилетит раньше'}]);
    });

    it('Прилет ожидается раньше, но известен выход на высадку', () => {
        expect(
            getMessageFromStatus(
                {
                    status: ThreadStatus.early,
                    checkInDesks: '200-202',
                    gate: '15',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([
            {text: 'Прилетит раньше'},
            {text: 'Выход на высадку 15'},
        ]);
    });

    it('Не вернет статусы для самолета, который прилетает/улетает позже 24 часов от текущего времени аэропорта', () => {
        expect(
            getMessageFromStatus(
                unknownStatus,
                StationEventList.arrival,
                25,
                terminal,
            ),
        ).toStrictEqual([]);
    });

    it('Вернет массив со строкой "Нет актуальной инормации о рейсе", когда event === arrival, status === unknown', () => {
        expect(
            getMessageFromStatus(
                unknownStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(unknownExpectMessage);
    });

    it('Вернет массив со строкой "Нет актуальной инормации о рейсе", когда event === departure, status === unknown, флаг при котором не нужно возвращать unknown статус не передан', () => {
        expect(
            getMessageFromStatus(
                unknownStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(unknownExpectMessage);
    });

    it('Вернет пустой массив для статуса canceled, когда event === arrival', () => {
        expect(
            getMessageFromStatus(
                canceledStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([]);
    });

    it('Вернет пустой массив для статуса canceled, когда event === departure', () => {
        expect(
            getMessageFromStatus(
                canceledStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([]);
    });

    it('Прилет по расписанию', () => {
        expect(
            getMessageFromStatus(
                onTimeStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
            ),
        ).toStrictEqual([onTimeArrivalExpectedMessages]);
    });

    it('Вернет массив с "Вылет по расписанию", когда event === departure, status === on-time', () => {
        expect(
            getMessageFromStatus(
                onTimeStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([{text: 'Вылет по расписанию'}]);
    });

    it('Выход на посадку', () => {
        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    gate: '15',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([
            onTimeArrivalExpectedMessages,
            {text: 'Выход на высадку 15'},
        ]);
    });

    it('Вернет массив с "Вылет раньше", "Стойки регистрации 200-202", когда event === departure, status === early, указаны стойки регистрации', () => {
        expect(
            getMessageFromStatus(
                earlyCheckInDesksStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(earlyCheckInDesksDepartureExpectedMessages);
    });

    it('Вернет массив с "Вылет раньше", "Стойки регистрации 200-202", "Выход на посадку 15", когда event === departure, status === early, указаны стойки регистрации, указан gate', () => {
        expect(
            getMessageFromStatus(
                earlyCheckInDesksGateStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(earlyCheckInDesksGateDepartureExpectedMessages);
    });

    it('Вернет пустой массив для статуса departed, когда event === arrival, самолет направлен в другой аэропорт, ', () => {
        expect(
            getMessageFromStatus(
                departedDivertedStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([]);
    });

    it('Вернет массив с "В полете", когда event ===  departure, самолет направлен в другой аэропорт', () => {
        expect(
            getMessageFromStatus(
                departedDivertedStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(departedDivertedDepartureExpectedMessages);
    });

    it('Вернет массив с "Прилетит с опозданием", когда event === arrival, status === delayed', () => {
        expect(
            getMessageFromStatus(
                delayedStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(delayedArrivalExpectedMessages);
    });

    it('Вернет пустой массив для статуса delayed, когда event === departure', () => {
        expect(
            getMessageFromStatus(
                delayedStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([]);
    });

    it('Вернет массив с "Прилетел", "Выдача багажа: лента 5", когда event === arrival, status === arrived, указана лента выдачи багажа', () => {
        expect(
            getMessageFromStatus(
                arrivedBaggageCarouselsStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(arrivalArrivedBaggageCarouselExpectedMessages);
    });

    it('Вернет пустой массив для статуса arrived, когда event === departure, указана лента выдачи багажа', () => {
        expect(
            getMessageFromStatus(
                arrivedBaggageCarouselsStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual([]);
    });

    it('Вернет массив с "Прилетел", "Выход на высадку 15", "Выдача багажа: лента 5", когда event === arrival, status === arrived, указана лента выдачи багажа, указан gate', () => {
        expect(
            getMessageFromStatus(
                arrivedBaggageCarouselsGateStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
            ),
        ).toStrictEqual(arrivalArrivedBaggageCarouselGateExpectedMessages);
    });

    it('Вернет информацию об изменениии терминала только если терминал по расписанию и измененнный известны и не равны друг другу', () => {
        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    actualTerminalName: '',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                '',
            ),
        ).toStrictEqual([onTimeArrivalExpectedMessages]);

        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    actualTerminalName: '',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                'A',
            ),
        ).toStrictEqual([onTimeArrivalExpectedMessages]);

        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    actualTerminalName: 'C',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                '',
            ),
        ).toStrictEqual([onTimeArrivalExpectedMessages]);

        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    actualTerminalName: 'C',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                'A',
            ),
        ).toStrictEqual([
            {
                text: 'Смена терминала',
                status: AdditionalThreadStatus.changeTerminal,
            },
            onTimeArrivalExpectedMessages,
        ]);
    });

    it('[параметр for informer = true] Если до вылета больше суток (24 часа), то вернет пустой массив', () => {
        expect(
            getMessageFromStatus(
                unknownStatus,
                StationEventList.arrival,
                25,
                terminal,
                true,
            ),
        ).toEqual([]);
    });

    it('[параметр for informer = true] Вернет массив со строкой "Нет актуальной информации о статусе", когда event === arrival, status === unknown', () => {
        const unknownMessage = [
            {
                text: 'Нет актуальной информации о статусе',
                status: ThreadStatus.unknown,
            },
        ];

        expect(
            getMessageFromStatus(
                unknownStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
                true,
            ),
        ).toEqual(unknownMessage);
    });

    it('[параметр for informer = true] Если статус cancelled, то вернет массив со строкой "Отменен"', () => {
        const canceledMessage = [
            {text: 'Отменен', status: ThreadStatus.cancelled},
        ];

        expect(
            getMessageFromStatus(
                canceledStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                terminal,
                true,
            ),
        ).toEqual(canceledMessage);
    });

    it('[параметр for informer = true] Если терминал изменился, вернет сообщение "Термминал {{terminalName}}"', () => {
        const terminalMessage = [
            {text: 'Терминал C', status: AdditionalThreadStatus.changeTerminal},
        ];

        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    actualTerminalName: 'C',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                'B',
                true,
            ),
        ).toEqual(terminalMessage);
    });

    it('[параметр for informer = true] Если передан терминал, вернет сообщение "Термминал {{terminalName}}"', () => {
        const terminalMessage = [{text: 'Терминал C', status: undefined}];

        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                'C',
                true,
            ),
        ).toEqual(terminalMessage);
    });

    it('[параметр for informer = true] Если передан терминал и он равен actualTerminalName, вернет сообщение "Термминал {{terminalName}}"', () => {
        const terminalMessage = [{text: 'Терминал C', status: undefined}];

        expect(
            getMessageFromStatus(
                {
                    ...onTimeStatus,
                    actualTerminalName: 'C',
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                'C',
                true,
            ),
        ).toEqual(terminalMessage);
    });

    it('[параметр for informer = true] Если статус onTime и event departure, вернет сообщение "Вылет по расписанию"', () => {
        const expectedMessage = [{text: 'Вылет по расписанию'}];

        expect(
            getMessageFromStatus(
                onTimeStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус delayed и event departure, вернет сообщение "Вылет задержан"', () => {
        const expectedMessage = [
            {text: 'Вылет задержан', status: ThreadStatus.delayed},
        ];

        expect(
            getMessageFromStatus(
                delayedStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус early и event departure, вернет сообщение "Вылет раньше"', () => {
        const expectedMessage = [{text: 'Вылет раньше'}];

        expect(
            getMessageFromStatus(
                {status: ThreadStatus.early} as IStationThreadStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус departed и event departure, вернет сообщение "Вылетел"', () => {
        const expectedMessage = [{text: 'Вылетел'}];

        expect(
            getMessageFromStatus(
                departedDivertedStatus,
                StationEventList.departure,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус early и event arrival, вернет сообщение "Прилетит раньше"', () => {
        const expectedMessage = [{text: 'Прилетит раньше'}];

        expect(
            getMessageFromStatus(
                {
                    status: ThreadStatus.early,
                },
                StationEventList.arrival,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус onTime и event arrival, вернет сообщение "Прилетит по расписанию"', () => {
        const expectedMessage = [{text: 'Прилетит по расписанию'}];

        expect(
            getMessageFromStatus(
                onTimeStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус delayed и event arrival, вернет сообщение "Прилетит с опозданием"', () => {
        const expectedMessage = [{text: 'Прилетит с опозданием'}];

        expect(
            getMessageFromStatus(
                delayedStatus,
                StationEventList.arrival,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });

    it('[параметр for informer = true] Если статус arrived и event arrival, вернет сообщение "Прилетел"', () => {
        const expectedMessage = [{text: 'Прилетел'}];

        expect(
            getMessageFromStatus(
                {status: ThreadStatus.arrived},
                StationEventList.arrival,
                hoursBeforeEvent,
                undefined,
                true,
            ),
        ).toEqual(expectedMessage);
    });
});
