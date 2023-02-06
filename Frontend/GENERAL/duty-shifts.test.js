import { JSON_REQUEST, requestJson } from './request-json';
import {
    DUTY_SHIFTS_UPDATE,
    DUTY_SHIFTS_SET_FILTERS,
    DUTY_SHIFTS_SET_LOADING,
    DUTY_SHIFTS_PATCH_REQUEST,
    DUTY_SHIFTS_PATCH,
    DUTY_SHIFTS_MERGE,
    DUTY_SHIFTS_REPLACEMENT_CREATE_REQUEST,
    DUTY_SHIFTS_REPLACEMENT_CREATE,
    DUTY_SHIFTS_REPLACEMENT_DELETE,
    DUTY_SHIFTS_REPLACEMENT_SET_LOADING,
    DUTY_SHIFTS_RESET,
    DUTY_SHIFTS_ERROR_RESET,
    updateDutyShifts,
    setLoading,
    patchDutyShift,
    _patchDutyShift,
    setApproveDutyShift,
    setReplacementLoading,
    _createDutyShiftsReplacement,
    createDutyShiftsReplacement,
    deleteDutyShiftsReplacement,
    mergeDutyShifts,
    shiftsFieldsWithReplaces,
} from './duty-shifts';
import handleActions from './duty-shifts';

describe('dutyShifts', () => {
    const scope = 'testScope';

    it('Should create JSON_REQUEST action in update', () => {
        const actual = updateDutyShifts(scope, {
            calendarId: 1,
            serviceId: 2,
            dateFrom: new Date(2019, 1, 18),
            dateTo: new Date(2019, 4, 18),
            personLogin: 'login',
            scheduleId: 3,
        });
        const expected = requestJson({
            pathname: '/back-proxy/api/v4/duty/shifts/',
            query: {
                calendar: 1,
                service: 2,
                person: 'login',
                date_from: '2019-02-18',
                date_to: '2019-05-18',
                schedule: 3,
                page: 1,
                page_size: 1000,
                fields: shiftsFieldsWithReplaces.join(','),
            },
        }, DUTY_SHIFTS_UPDATE, { scope });

        expect(actual.payload).toContainEqual(expected);
    });

    it('Should request all roles with problems using "__problems__" as scheduleId', () => {
        const serialActions = updateDutyShifts(scope, {
            dateFrom: '2019-02-18',
            dateTo: '2019-05-18',
            scheduleId: '__problems__',
        });
        const requestAction = serialActions.payload.find(action => action.type === JSON_REQUEST);
        const actual = requestAction.payload.query;

        expect(actual).toMatchObject({ has_problems: true });
        expect(actual).toEqual(expect.not.objectContaining({ schedule: expect.anything() }));
    });

    it('Should create payload for settings shift loading state', () => {
        expect(setLoading(true, scope)).toEqual({
            type: DUTY_SHIFTS_SET_LOADING,
            payload: true,
            meta: { scope },
        });
    });

    it('Should create payload for duty shift patch request', () => {
        expect(patchDutyShift(scope, 1, { person: 'login@' })).toEqual({
            type: DUTY_SHIFTS_PATCH_REQUEST,
            payload: {
                scope,
                shiftId: 1,
                data: { person: 'login@' },
            },
        });
    });

    it('Should create JSON_REQUEST action in patch', () => {
        const actual = _patchDutyShift(scope, 1, null, null, { person: 'login@' });
        const expected = requestJson({
            pathname: '/back-proxy/api/v3/duty/shifts/1/',
            method: 'PATCH',
            data: {
                person: 'login@',
            },
        }, DUTY_SHIFTS_PATCH, { scope });

        expect(actual).toEqual(expected);
    });

    it('Should create payload for setting replacement loading state', () => {
        expect(setReplacementLoading(scope, 1, 42, true)).toEqual({
            type: DUTY_SHIFTS_REPLACEMENT_SET_LOADING,
            payload: {
                parentId: 1,
                replacementId: 42,
                loading: true,
            },
            meta: { scope },
        });
    });

    it('Should create payload for creating a replacement', () => {
        const data = {
            id: 10,
            person: { login: 'john.doe' },
            startDate: new Date(2000, 0, 1),
            endDate: new Date(2000, 0, 2),
        };

        expect(createDutyShiftsReplacement(scope, 1, data)).toEqual({
            type: DUTY_SHIFTS_REPLACEMENT_CREATE_REQUEST,
            payload: {
                scope,
                shiftId: 1,
                data,
            },
        });
    });

    it('Should create JSON_REQUEST action for creating a replacement', () => {
        const actual = _createDutyShiftsReplacement(scope, 1, null, null, {
            id: 10,
            person: { login: 'john.doe' },
            startDate: new Date(Date.UTC(2000, 0, 1)),
            endDate: new Date(Date.UTC(2000, 0, 2)),
        });
        const expected = requestJson({
            pathname: '/back-proxy/api/v3/duty/shifts/',
            method: 'POST',
            data: {
                id: 10,
                replace_for: 1,
                person: 'john.doe',
                start_datetime: '2000-01-01T03:00:00.000+03:00',
                end_datetime: '2000-01-02T03:00:00.000+03:00',
            },
        }, DUTY_SHIFTS_REPLACEMENT_CREATE, { scope, parentId: 1, restoredId: 10 });

        expect(actual).toEqual(expected);
    });

    it('Should create JSON_REQUEST action for deleting a replacement', () => {
        const actual = deleteDutyShiftsReplacement(scope, 1, 10);
        const expected = requestJson({
            pathname: '/back-proxy/api/v3/duty/shifts/10/',
            method: 'DELETE',
        }, DUTY_SHIFTS_REPLACEMENT_DELETE, { scope, parentId: 1, replacementId: 10 });

        expect(actual.payload).toContainEqual(expected);
    });

    it('Should create payload for approving a shift', () => {
        expect(setApproveDutyShift(scope, 1, true)).toEqual({
            type: DUTY_SHIFTS_PATCH_REQUEST,
            payload: {
                scope,
                shiftId: 1,
                data: { is_approved: true },
            },
        });
    });

    it('Should create payload for merging scopes', () => {
        expect(mergeDutyShifts(scope, 'anotherScope')).toEqual({
            payload: {
                source: scope,
                target: 'anotherScope',
            },
            type: DUTY_SHIFTS_MERGE,
        });
    });

    it('Should handle DUTY_SHIFTS_SET_LOADING action', () => {
        expect(handleActions({}, {
            type: DUTY_SHIFTS_SET_LOADING,
            payload: true,
            meta: { scope },
        })).toEqual({
            [scope]: { loading: true },
        });
    });

    it('Should handle DUTY_SHIFTS_SET_FILTERS action', () => {
        const filters = {
            calendarId: 1,
            serviceId: 2,
            dateFrom: '2019-02-18',
            dateTo: '2019-05-18',
            personLogin: 'login',
        };

        expect(handleActions({}, {
            type: DUTY_SHIFTS_SET_FILTERS,
            payload: filters,
            meta: { scope },
        })).toEqual({
            [scope]: { filters },
        });
    });

    it('Should handle DUTY_SHIFTS_UPDATE action', () => {
        expect(handleActions({}, {
            type: DUTY_SHIFTS_UPDATE,
            payload: {
                results: ['some data'],
            },
            error: false,
            meta: { from: { meta: { scope } } },
        })).toEqual({
            [scope]: {
                error: null,
                data: ['some data'],
            },
        });
    });

    it('Should handle DUTY_SHIFTS_UPDATE error', () => {
        expect(handleActions({}, {
            type: DUTY_SHIFTS_UPDATE,
            payload: { message: 'error' },
            error: true,
            meta: { from: { meta: { scope } } },
        })).toEqual({
            [scope]: {
                error: { message: 'error' },
                data: [],
            },
        });
    });

    it('Should handle DUTY_SHIFTS_PATCH action', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [{ id: 4 }],
                },
            },
            {
                type: DUTY_SHIFTS_PATCH,
                payload: {
                    id: 4,
                    is_approved: true,
                },
                error: false,
                meta: { from: { meta: { scope } } },
            },
        );

        const expected = {
            [scope]: {
                error: null,
                data: [{
                    id: 4,
                    is_approved: true,
                }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_PATCH action with empty state', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [],
                },
            },
            {
                type: DUTY_SHIFTS_PATCH,
                payload: {
                    id: 4,
                    is_approved: true,
                },
                error: false,
                meta: { from: { meta: { scope } } },
            },
        );

        const expected = {
            [scope]: {
                error: null,
                data: [],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_PATCH error', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [{ id: 4 }],
                },
            },
            {
                type: DUTY_SHIFTS_PATCH,
                payload: { message: 'error' },
                error: true,
                meta: { from: { meta: { scope } } },
            },
        );

        const expected = {
            [scope]: {
                error: { message: 'error' },
                data: [{ id: 4 }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_MERGE action', () => {
        const actual = handleActions(
            {
                src: { commonField: 42 },
                dst: { commonField: 146, uniqueField: 1 },
            },
            {
                type: DUTY_SHIFTS_MERGE,
                payload: {
                    source: 'src',
                    target: 'dst',
                },
            },
        );

        const expected = {
            src: { commonField: 42 },
            dst: { commonField: 42, uniqueField: 1 },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_REPLACEMENT_CREATE action for restoring a replacement', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [
                        { id: 3, replaces: [{ id: 10, isDeleted: true, loading: false }] },
                        {
                            id: 4, replaces: [
                                { id: 10, isDeleted: true, loading: true },
                                { id: 11, isDeleted: true, loading: false },
                            ],
                        },
                    ],
                },
            },
            {
                type: DUTY_SHIFTS_REPLACEMENT_CREATE,
                payload: {
                    id: 42,
                },
                error: false,
                meta: { from: { meta: { scope, parentId: 4, restoredId: 10 } } },
            },
        );

        const expected = {
            [scope]: {
                data: [
                    {
                        id: 3,
                        replaces: [{ id: 10, isDeleted: true, loading: false }],
                    }, {
                        id: 4,
                        replaces: [
                            { id: 42, isDeleted: false, loading: false },
                            { id: 11, isDeleted: true, loading: false },
                        ],
                    },
                ],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_REPLACEMENT_CREATE action for creating a replacement', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [{ id: 4, replaces: [{ id: 10, isDeleted: true, loading: false }] }],
                },
            },
            {
                type: DUTY_SHIFTS_REPLACEMENT_CREATE,
                payload: {
                    id: 42,
                },
                error: false,
                meta: { from: { meta: { scope, parentId: 4 } } },
            },
        );

        const expected = {
            [scope]: {
                data: [{
                    id: 4,
                    replaces: [
                        { id: 10, isDeleted: true, loading: false },
                        { id: 42, isDeleted: false, loading: false },
                    ],
                }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_REPLACEMENT_CREATE error', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [{ id: 4 }],
                },
            },
            {
                type: DUTY_SHIFTS_REPLACEMENT_CREATE,
                payload: { message: 'error' },
                error: true,
                meta: { from: { meta: { scope, parentId: 4 } } },
            },
        );

        const expected = {
            [scope]: {
                error: { message: 'error' },
                data: [{ id: 4 }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_REPLACEMENT_DELETE action', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [{ id: 4, replaces: [{ id: 10 }, { id: 11 }] }],
                },
            },
            {
                type: DUTY_SHIFTS_REPLACEMENT_DELETE,
                payload: {
                    id: 11,
                },
                error: false,
                meta: { from: { meta: { scope, parentId: 4, replacementId: 11 } } },
            },
        );

        const expected = {
            [scope]: {
                data: [{
                    id: 4,
                    replaces: [{ id: 10 }, { id: 11, isDeleted: true }],
                }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_REPLACEMENT_DELETE error', () => {
        const actual = handleActions(
            {
                [scope]: {
                    data: [{ id: 4, replaces: [{ id: 10 }] }],
                },
            },
            {
                type: DUTY_SHIFTS_REPLACEMENT_DELETE,
                payload: { message: 'error' },
                error: true,
                meta: { from: { meta: { scope, parentId: 4, replacementId: 10 } } },
            },
        );

        const expected = {
            [scope]: {
                error: { message: 'error' },
                data: [{ id: 4, replaces: [{ id: 10 }] }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_REPLACEMENT_SET_LOADING action', () => {
        const actual = handleActions({
            [scope]: {
                data: [{ id: 1, replaces: [{ id: 2 }] }],
            },
        }, {
            type: DUTY_SHIFTS_REPLACEMENT_SET_LOADING,
            payload: {
                parentId: 1,
                replacementId: 2,
                loading: true,
            },
            meta: { scope },
        });

        const expected = {
            [scope]: {
                data: [{ id: 1, replaces: [{ id: 2, loading: true }] }],
            },
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle DUTY_SHIFTS_RESET action', () => {
        const _Date = Date;
        Date = jest.fn(); // eslint-disable-line no-global-assign
        Date.UTC = _Date.UTC;
        Date.mockImplementation(() => new _Date(_Date.UTC(2000, 0, 1)));

        const actual = handleActions({}, { type: DUTY_SHIFTS_RESET });

        const expected = {
            calendar: {
                filters: {
                    dateFrom: new Date(),
                    scheduleId: null,
                    scale: 'day',
                },
                loading: false,
                error: null,
                data: [],
            },
            'schedule-edit': {
                filters: {
                    dateFrom: new Date(),
                    scheduleId: null,
                    scale: 'day',
                },
                loading: false,
                error: null,
                data: [],
            },
            holidays: [],
        };

        expect(actual).toEqual(expected);

        Date = _Date; // eslint-disable-line no-global-assign
    });

    it('Should handle DUTY_SHIFTS_ERROR_RESET action', () => {
        const actual = handleActions(
            {
                [scope]: {
                    error: { message: 'error' },
                    data: 'some data',
                },
            },
            {
                type: DUTY_SHIFTS_ERROR_RESET,
                payload: scope,
            },
        );

        const expected = {
            [scope]: {
                error: null,
                data: 'some data',
            },
        };

        expect(actual).toEqual(expected);
    });
});
