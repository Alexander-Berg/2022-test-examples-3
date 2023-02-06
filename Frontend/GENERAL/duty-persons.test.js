import { requestJson } from './request-json';
import handleActions from './duty-persons';
import { DUTY_PERSONS_GET, DUTY_PERSONS_GET_BY_SCHEDULE, getPersons, getPersonsBySchedule } from './duty-persons';

describe('Should create JSON_REQUEST action', () => {
    it('for getPersons', () => {
        const actual = getPersons(42, 1);
        const expected = requestJson({
            pathname: '/back-proxy/api/v3/duty/allowforduty/',
            query: { service: 42, role: 1, page_size: 1000 },
        }, DUTY_PERSONS_GET, { roleId: 1 });

        expect(actual.payload).toContainEqual(expected);
    });

    it('for getPersonsBySchedule', () => {
        const actual = getPersonsBySchedule(42, 5684);
        const expected =
            requestJson({
                pathname: '/back-proxy/api/v3/duty/allowforduty/',
                query: { service: 42, schedule: 5684, page_size: 1000 },
            }, DUTY_PERSONS_GET_BY_SCHEDULE, { scheduleId: 5684 });

        expect(actual.payload).toContainEqual(expected);
    });
});

describe('Should handle actions', () => {
    it('DUTY_PERSONS_GET', () => {
        const personsByRole = [
            { id: 23030, login: 'user1', name: { ru: 'Имя1 Фамилия1', en: 'Name1 Surname1' } },
            { id: 57731, login: 'user2', name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' } },
            { id: 36980, login: 'user3', name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' } },
            { id: 14690, login: 'user4', name: { ru: 'Имя4 Фамилия4', en: 'Name4 Surname4' } },
        ];

        expect(handleActions({}, {
            type: DUTY_PERSONS_GET,
            payload: {
                results: personsByRole,
            },
            error: false,
            meta: { from: { meta: { roleId: 8 } } },
        })).toEqual({
            persons: {
                ['8']: personsByRole,
            },
            rolesErrors: {},
        });
    });

    it('DUTY_PERSONS_GET_BY_SCHEDULE', () => {
        const personsBySchedule = [
            { id: 57731, login: 'user2', name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' }, order: 1, active_duty: true, start_with: false },
            { id: 23030, login: 'user1', name: { ru: 'Имя1 Фамилия1', en: 'Name1 Surname1' }, order: 0, active_duty: false, start_with: false },
            { id: 36980, login: 'user3', name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' }, order: 2, active_duty: false, start_with: true },
            { id: 14690, login: 'user4', name: { ru: 'Имя4 Фамилия4', en: 'Name4 Surname4' }, order: null, active_duty: false, start_with: false },
        ];

        const expectedOrders = [
            { id: 36980, login: 'user3', name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' }, order: 2, active_duty: false, start_with: true },
            { id: 23030, login: 'user1', name: { ru: 'Имя1 Фамилия1', en: 'Name1 Surname1' }, order: 0, active_duty: false, start_with: false },
            { id: 57731, login: 'user2', name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' }, order: 1, active_duty: true, start_with: false },
        ];

        const expectedActiveOrders = [
            { id: 57731, login: 'user2', name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' }, order: 1, active_duty: true, start_with: false },
        ];

        const expectedMissingOrders = [
            { id: 14690, login: 'user4', name: { ru: 'Имя4 Фамилия4', en: 'Name4 Surname4' }, order: null, active_duty: false, start_with: false },
        ];

        expect(handleActions({}, {
            type: DUTY_PERSONS_GET_BY_SCHEDULE,
            payload: {
                results: personsBySchedule,
            },
            error: false,
            meta: { from: { meta: { scheduleId: 50 } } },
        })).toEqual({
            schedules: {
                ['50']: {
                    orders: expectedOrders,
                    activeOrders: expectedActiveOrders,
                    missingOrders: expectedMissingOrders,
                },
            },
            schedulesErrors: {},
        });
    });
});
