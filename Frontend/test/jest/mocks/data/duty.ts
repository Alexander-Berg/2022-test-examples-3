import { DutyShifts, Filters, Holiday, Shift } from '~/src/features/Duty/redux/DutyShifts.types';
import { DutySchedules, Schedule } from '~/src/features/Duty/redux/DutySchedules.types';
import { getPersonMock, getServiceMock } from './common';

export const getDutyShiftMock = (id: number, scheduleId: number, data?: Partial<Shift>): Shift => ({
    id,
    person: getPersonMock(),
    problems_count: 0,
    schedule: {
        id: scheduleId,
    },
    is_approved: true,
    start: '2020-01-01',
    end: '2020-01-07',
    replaces: [],
    start_datetime: '2020-01-01T00:00:00+03:00',
    end_datetime: '2020-01-08T00:00:00+03:00',
    ...data,
});

export const getDutyScheduleMock = (id: number, data: Omit<Schedule,
    | 'id'
    | 'service'
    | 'name'
    | 'description'
    | 'slug'
    | 'role'
    | 'roleOnDuty'
    | 'personsCount'
    | 'startDate'
    | 'startTime'
    | 'duration'
    | 'autoapproveTimedelta'
    | 'dutyOnHolidays'
    | 'dutyOnWeekends'
    | 'considerOtherSchedules'
    | 'showInStaff'
    | 'needOrder'>,
): Schedule => ({
    id,
    service: getServiceMock(1),
    name: 'Mock Schedule',
    description: '',
    slug: 'mock_schedule',
    role: undefined,
    roleOnDuty: undefined,
    personsCount: 1,
    startDate: new Date('2020-01-01T00:00:00.000+03:00'),
    startTime: '00:00',
    duration: 7,
    autoapproveTimedelta: 7,
    dutyOnHolidays: true,
    dutyOnWeekends: true,
    considerOtherSchedules: false,
    showInStaff: false,
    needOrder: false,
    ...data,
});

export const getFilters = (data?: Partial<Filters>): Filters => ({
    serviceId: 1,
    dateFrom: new Date('2020-01-01T00:00:00+03:00'),
    dateTo: new Date('2020-06-01T00:00:00+03:00'),
    scheduleId: null,
    // @ts-expect-error ABC-11164
    person: [],
    scale: 'day',
    ...data,
});

export const getDutyScopeMock = (data?: Partial<DutyShifts['calendar']>): DutyShifts['calendar'] => ({
    data: [
        getDutyShiftMock(1, 10),
        getDutyShiftMock(2, 11),
    ],
    error: null,
    filters: getFilters(),
    hasChanges: false,
    loading: false,
    ...data,
});

export const getDutyShiftsMock = (dutyShifts?: Partial<DutyShifts['calendar']>, holidays: Holiday[] = []): DutyShifts => {
    const dutyScope = {
        ...getDutyScopeMock(),
        ...dutyShifts,
    };

    return {
        calendar: dutyScope,
        'schedule-edit': dutyScope,
        holidays,
    };
};

export const getDutySchedulesMock = (data?: Partial<DutySchedules>): DutySchedules => ({
    collection: [
        // @ts-expect-error ABC-11164
        getDutyScheduleMock(10),
        // @ts-expect-error ABC-11164
        getDutyScheduleMock(11),
    ],
    errors: [],
    loading: false,
    inited: true,
    dutyPersons: [],
    dutyPersonsLoading: false,
    ...data,
});

// это заготовка для полноценного мока
// нужно дописать тип и добавить эталонные данные, и удалить этот комментарий
// используешь этот генератор — скорее всего, нужно придумать и добавить данные тут, а не передавать аргументом
// чтобы в тестах можно было ожидать стандартные отсутствия, не дублируя моки в разных тестах
export const getDutyAbsencesMock = (data?: Partial<{}>) => ({
    data: [],
    error: null,
    filters: getFilters(),
    loading: false,
    ...data,
});
