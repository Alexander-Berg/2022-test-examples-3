import { PARALLEL_DISPATCH } from 'tools-access-react-redux/src/parallel';
import { requestJson } from '~/src/abc/react/redux/request-json';

import {
    DUTY_PERSONS_UPDATE,
    DUTY_SCHEDULES_SET_LOADING,
    DUTY_SCHEDULES_ERRORS_RESET,
    DUTY_SCHEDULES_CLIENT_DATA_RESET,
    DUTY_SCHEDULES_UPDATE,
    DUTY_SCHEDULE_UPDATE,
    DUTY_SCHEDULE_CREATE,
    DUTY_SCHEDULE_PATCH,
    DUTY_SCHEDULE_DELETE,
    CRITICAL_FIELDS_UPDATE_REQUEST,
    CRITICAL_FIELDS_UPDATE,
    deleteDutySchedule,
    updateDutySchedules,
    upsertDutySchedule,
    updateDutyPersons,
    requestCriticalFieldsUpdate,
    updateCriticalFields,
    resetCriticalFields,
} from './DutySchedules.redux';
import handleActions from './DutySchedules.redux';

const SECONDS_IN_DAY = 24 * 60 * 60;

describe('dutySchedules', () => {
    const parsedSettings = {
        id: 1,
        service: { id: 42 },
        name: 'name',
        slug: 'slug',
        role: { id: 3, name: 'role' },
        personsCount: 1,
        requesterInDuty: false,
        startDate: new Date('2000-01-01'),
        duration: 7,
        dutyOnHolidays: false,
        dutyOnWeekends: false,
        considerOtherSchedules: true,
        showInStaff: true,
        needOrder: false,
        trackerQueueId: null,
        trackerComponentId: null,
    };

    describe('Should create JSON_REQUEST action', () => {
        it('for updateDutySchedules', () => {
            const actual = updateDutySchedules(42);
            const expected = requestJson({
                pathname: '/back-proxy/api/frontend/duty/schedules/',
                query: { service: 42, page_size: 60 },
            }, DUTY_SCHEDULES_UPDATE);

            expect(actual.payload).toContainEqual(expected);
        });

        it('for updateDutyPersons', () => {
            const actual = updateDutyPersons(42);
            const expected = requestJson({
                pathname: '/back-proxy/api/v4/duty/allowforduty/',
                query: { service: 42, fields: 'id,login,name', page_size: 1000 },
            }, DUTY_PERSONS_UPDATE);

            expect(actual.payload).toContainEqual(expected);
        });

        it('for _createDutySchedule', () => {
            const newSettings = {
                service: { id: 42 },
                name: 'name2',
                slug: 'slug2',
                role: { id: 3, name: 'role' },
                roleOnDuty: 4,
                personsCount: 2,
                startDate: new Date('2000-01-02'),
                duration: 5,
                autoapproveTimedelta: 7,
                dutyOnHolidays: true,
                dutyOnWeekends: true,
                considerOtherSchedules: false,
                showInStaff: false,
                needOrder: false,
                startTime: '12:20',
                trackerQueueId: null,
                trackerComponentId: null,
            };

            const actual = upsertDutySchedule(42, newSettings, null);

            const expected = {
                type: PARALLEL_DISPATCH,
                payload: expect.arrayContaining([
                    requestJson({
                        pathname: '/back-proxy/api/v3/duty/schedules/',
                        method: 'POST',
                        data: {
                            service: 42,
                            name: 'name2',
                            slug: 'slug2',
                            role: 3,
                            role_on_duty: 4,
                            persons_count: 2,
                            start_date: '2000-01-02',
                            duration: 5 * SECONDS_IN_DAY,
                            autoapprove_timedelta: 7 * SECONDS_IN_DAY,
                            duty_on_holidays: true,
                            duty_on_weekends: true,
                            consider_other_schedules: false,
                            show_in_staff: false,
                            algorithm: 'no_order',
                            start_time: '12:20',
                            tracker_queue_id: null,
                            tracker_component_id: null,
                        },
                    }, DUTY_SCHEDULE_CREATE),
                ]),
            };

            expect(actual.payload).toContainEqual(expected);
        });

        it('for _patchDutySchedule', () => {
            const newSettings =
            {
                id: 1,
                service: { id: 42 },
                name: 'name2',
                slug: 'slug2',
                role: { id: 3, name: 'role' },
                roleOnDuty: 4,
                personsCount: 2,
                startDate: new Date('2000-01-02'),
                duration: 5,
                autoapproveTimedelta: 7,
                dutyOnHolidays: true,
                dutyOnWeekends: true,
                considerOtherSchedules: false,
                showInStaff: false,
                needOrder: false,
                startTime: '13:38',
                trackerQueueId: null,
                trackerComponentId: null,
            };

            const actual = upsertDutySchedule(42, newSettings, parsedSettings);

            const expected = {
                type: PARALLEL_DISPATCH,
                payload: expect.arrayContaining([
                    requestJson({
                        pathname: '/back-proxy/api/v3/duty/schedules/1/',
                        method: 'PATCH',
                        data: {
                            service: 42,
                            id: 1,
                            name: 'name2',
                            slug: 'slug2',
                            role: 3,
                            role_on_duty: 4,
                            persons_count: 2,
                            start_date: '2000-01-02',
                            duration: 5 * SECONDS_IN_DAY,
                            autoapprove_timedelta: 7 * SECONDS_IN_DAY,
                            duty_on_holidays: true,
                            duty_on_weekends: true,
                            consider_other_schedules: false,
                            show_in_staff: false,
                            algorithm: 'no_order',
                            start_time: '13:38',
                            tracker_queue_id: null,
                            tracker_component_id: null,
                        },
                    }, DUTY_SCHEDULE_PATCH),
                ]),
            };

            expect(actual.payload).toContainEqual(expected);
        });

        it('for _deleteDutySchedule', () => {
            const actual = deleteDutySchedule(42, parsedSettings);
            const expected =
                requestJson({
                    pathname: '/back-proxy/api/v3/duty/schedules/1/',
                    method: 'DELETE',
                }, DUTY_SCHEDULE_DELETE);

            expect(actual.payload).toContainEqual(expected);
        });
    });

    it('Should create payload for critical fields update request', () => {
        expect(requestCriticalFieldsUpdate(42, [{ foo: 'bar' }])).toEqual({
            type: CRITICAL_FIELDS_UPDATE_REQUEST,
            payload: {
                serviceId: 42,
                diff: [{ foo: 'bar' }],
            },
        });
    });

    it('Should create payload for critical fields update', () => {
        expect(updateCriticalFields('anything')).toEqual({
            type: CRITICAL_FIELDS_UPDATE,
            payload: 'anything',
        });
    });

    it('Should create payload for critical fields reset', () => {
        expect(resetCriticalFields()).toEqual({
            type: CRITICAL_FIELDS_UPDATE,
            payload: undefined,
        });
        expect(resetCriticalFields('anything')).toEqual({
            type: CRITICAL_FIELDS_UPDATE,
            payload: undefined,
        });
    });

    describe('Should handle actions', () => {
        const rawSettings = [
            {
                id: 1,
                service: { id: 42 },
                name: 'name',
                slug: 'slug',
                role: { id: 3, name: 'role' },
                roleOnDuty: 4,
                persons_count: 1,
                start_date: '2000-01-01',
                duration: 7,
                duty_on_holidays: false,
                duty_on_weekends: false,
                consider_other_schedules: true,
                show_in_staff: true,
                tracker_queue_id: null,
                tracker_component_id: null,
            },
        ];

        it('DUTY_SCHEDULES_UPDATE', () => {
            expect(handleActions({}, {
                type: DUTY_SCHEDULES_UPDATE,
                payload: {
                    results: rawSettings,
                },
                error: false,
            })).toEqual({
                collection: [parsedSettings],
                inited: true,
            });
        });

        it('DUTY_SCHEDULE_UPDATE', () => {
            expect(handleActions({}, {
                type: DUTY_SCHEDULE_UPDATE,
                payload: {
                    ...rawSettings[0],
                },
                error: false,
            })).toEqual({
                clientData: parsedSettings,
                single: parsedSettings,
                inited: true,
                scheduleError: null,
            });
        });

        it('DUTY_SCHEDULES_UPDATE error', () => {
            const error = new Error();

            expect(handleActions({ errors: [] }, {
                type: DUTY_SCHEDULES_UPDATE,
                payload: error,
                error: true,
            })).toEqual({ errors: [error] });
        });

        it('DUTY_SCHEDULE_CREATE', () => {
            const initialStore = { clientData: { property: 'value' } };
            const expected = { clientData: { id: 1, property: 'value' } };

            const actual = handleActions(initialStore, {
                type: DUTY_SCHEDULE_CREATE,
                payload: {
                    ...rawSettings[0],
                    id: 1,
                    schedules: null, // заодно тест на парсинг данных без тегов
                },
                error: false,
            });

            expect(actual).toEqual(expected);
        });

        it('DUTY_SCHEDULE_CREATE error', () => {
            const error = new Error();

            const initialStore = { scheduleError: undefined };
            const expected = { scheduleError: error };

            expect(handleActions(initialStore, {
                type: DUTY_SCHEDULE_CREATE,
                payload: error,
                error: true,
            })).toEqual(expected);
        });

        it('DUTY_SCHEDULE_PATCH', () => {
            const initialStore = { single: [{ property: 'value' }] };

            const actual = handleActions(initialStore, {
                type: DUTY_SCHEDULE_PATCH,
                payload: rawSettings,
                error: false,
            });

            expect(actual).toEqual(initialStore);
        });

        it('DUTY_SCHEDULE_PATCH error', () => {
            const error = new Error();

            const initialStore = { scheduleError: undefined };
            const expected = { scheduleError: error };

            expect(handleActions(initialStore, {
                type: DUTY_SCHEDULE_PATCH,
                payload: error,
                error: true,
            })).toEqual(expected);
        });

        it('DUTY_SCHEDULE_DELETE', () => {
            const initialStore = { single: [{ property: 'value' }] };

            const actual = handleActions(initialStore, { type: DUTY_SCHEDULE_DELETE });

            expect(actual).toEqual(initialStore);
        });

        it('DUTY_SCHEDULE_DELETE error', () => {
            const error = new Error();

            expect(handleActions({ errors: [] }, {
                type: DUTY_SCHEDULE_DELETE,
                payload: error,
                error: true,
            })).toEqual({ errors: [error] });
        });

        it('DUTY_SCHEDULES_SET_LOADING', () => {
            expect(handleActions(
                { loading: true },
                {
                    type: DUTY_SCHEDULES_SET_LOADING,
                    payload: false,
                },
            )).toEqual({ loading: false });
        });

        it('DUTY_SCHEDULES_ERRORS_RESET', () => {
            const initialStore = {
                single: { property: 'value' },
                errors: [new Error()],
                scheduleError: new Error(),
            };

            const actual = handleActions(initialStore, { type: DUTY_SCHEDULES_ERRORS_RESET });

            const expected = {
                single: { property: 'value' },
                errors: [],
                scheduleError: null,
            };

            expect(actual).toEqual(expected);
        });

        it('DUTY_SCHEDULES_CLIENT_DATA_RESET', () => {
            const initialStore = { single: { property: 'value' } };

            const actual = handleActions(initialStore, { type: DUTY_SCHEDULES_CLIENT_DATA_RESET });

            const expected = {
                single: { property: 'value' },
                clientData: { property: 'value' },
            };

            expect(actual).toEqual(expected);
        });

        it('CRITICAL_FIELDS_UPDATE', () => {
            const actual = handleActions({}, {
                type: CRITICAL_FIELDS_UPDATE,
                payload: { foo: 'bar' },
            });

            const expected = {
                criticalFields: { foo: 'bar' },
            };

            expect(actual).toEqual(expected);
        });
    });
});
