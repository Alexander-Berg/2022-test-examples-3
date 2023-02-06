import {momentTimezone as moment} from '../../../../reexports';

import {FilterTransportType} from '../../transportType';

import getNewPlanDescriptionData from '../getNewPlanDescriptionData';

const trainContext = {
    transportType: FilterTransportType.train,
    time: {
        // 2018-04-12
        now: 1523523855000,
    },
    when: {
        date: '1970-01-01',
    },
};

const tooEarlyContext = {
    transportType: FilterTransportType.suburban,
    time: {
        // 2018-04-12
        now: 1523523855000,
    },
    when: {
        date: '1970-01-01',
    },
};

const tooLateContext = {
    transportType: FilterTransportType.suburban,
    time: {
        // 2018-12-12
        now: 1544605455000,
    },
    when: {
        date: '1970-01-01',
    },
};

const tooEarlyWhenContext = {
    transportType: FilterTransportType.suburban,
    time: {
        // 2018-11-12
        now: 1542013455000,
    },
    when: {
        date: '2018-11-12',
    },
};

const correctContext = {
    transportType: FilterTransportType.suburban,
    time: {
        // 2018-11-12
        now: 1542013455000,
    },
    when: {
        date: '2018-12-12',
    },
};

const newPlanDate = moment({M: 11, d: 1, y: 2018}).day(14);

describe('getNewPlanDescriptionData', () => {
    it('Должен вернуть false если неподходящий тип транспорта', () => {
        expect(getNewPlanDescriptionData(trainContext)).toEqual({
            newPlanDate,
            showNewPlanDescription: false,
        });
    });

    it('Должен вернуть false, если время вызова раньше начала появления описания', () => {
        expect(getNewPlanDescriptionData(tooEarlyContext)).toEqual({
            newPlanDate,
            showNewPlanDescription: false,
        });
    });

    it('Должен вернуть false, если время вызова позже даты вступления в силу нового плана', () => {
        expect(getNewPlanDescriptionData(tooLateContext)).toEqual({
            newPlanDate,
            showNewPlanDescription: false,
        });
    });

    it('Должен вернуть false, если дата на которую сделан запрос раньше вступления в силу нового плана', () => {
        expect(getNewPlanDescriptionData(tooEarlyWhenContext)).toEqual({
            newPlanDate,
            showNewPlanDescription: false,
        });
    });

    it('Должен вернуть true, если все параметры подходят', () => {
        expect(getNewPlanDescriptionData(correctContext)).toEqual({
            newPlanDate,
            showNewPlanDescription: true,
        });
    });
});
