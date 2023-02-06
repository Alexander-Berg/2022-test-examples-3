import React from 'react';
import 'babel-polyfill';
import { mount } from 'enzyme';
import inherit from 'inherit';

import TaDatepicker from 'b:ta-datepicker';
import TaSuggest from 'b:ta-suggest';
import { _Internal_SettingsContainer } from './Settings.container';

describe('Should init', () => {
    it('with default state when no data is provided', () => {
        const wrapper = mount(
            <_Internal_SettingsContainer
                onCancel={jest.fn()}
                onSubmit={jest.fn()}
                service={{ id: 42 }}
                dutySchedules={{
                    single: {},
                    clientData: {},
                    scheduleError: null,
                }}
                dutyPersons={{
                    persons: {},
                    schedules: {},
                    rolesErrors: {},
                    schedulesErrors: {},
                }}
                upsertDutySchedule={jest.fn()}
                resetDutySchedulesErrors={jest.fn()}
                resetDutySchedulesClientData={jest.fn()}
                getPersons={jest.fn()}
                getPersonsBySchedule={jest.fn()}
                updateDutySchedule={jest.fn()}
                updateServiceOptions={jest.fn()}
                deleteDutySchedule={jest.fn()}
                scheduleKey="123"
            />,
        );

        expect(wrapper.state()).toEqual({
            settingsEqual: true,
            isDeleteDutyCalendarOpen: false,
            waitToRedirect: false,
            schedule: {},
            propsSchedule: {},
            personsError: null,
            trackerPermissions: {
                queueAccess: true,
                queueAccessMessage: '',
                inited: true,
            },
        });

        wrapper.unmount();
    });

    it('with a state with provided settings', () => {
        const data = {
            id: 35,
            name: 'release',
            slug: 'release',
            role: null,
            roleOnDuty: null,
            personsCount: 1,
            considerOtherSchedules: true,
            duration: 5,
            dutyOnHolidays: true,
            dutyOnWeekends: true,
            startDate: new Date('2019-01-01'),
            startTime: '12:30',
            needOrder: false,
        };

        const wrapper = mount(
            <_Internal_SettingsContainer
                dutySchedules={{
                    single: data,
                    clientData: data,
                }}
                dutyPersons={{
                    persons: {},
                    schedules: {
                        ['35']: {
                            orders: [],
                            missingOrders: [],
                            activeOrders: [],
                        },
                    },
                    rolesErrors: {},
                    schedulesErrors: {},
                }}
                onCancel={jest.fn()}
                onSubmit={jest.fn()}
                service={{ id: 42 }}
                upsertDutySchedule={jest.fn()}
                resetDutySchedulesErrors={jest.fn()}
                resetDutySchedulesClientData={jest.fn()}
                getPersons={jest.fn()}
                getPersonsBySchedule={jest.fn()}
                updateDutySchedule={jest.fn()}
                updateServiceOptions={jest.fn()}
                deleteDutySchedule={jest.fn()}
                scheduleKey="123"
            />,
        );

        expect(wrapper.state('schedule')).toEqual({
            id: 35,
            name: 'release',
            slug: 'release',
            role: null,
            roleOnDuty: null,
            personsCount: 1,
            considerOtherSchedules: true,
            duration: 5,
            dutyOnHolidays: true,
            dutyOnWeekends: true,
            startDate: new Date('2019-01-01'),
            startTime: '12:30',
            needOrder: false,
            activeOrders: [],
        });

        wrapper.unmount();
    });
});

describe('Should handle form update', () => {
    const onCancel = jest.fn();
    const getPersons = jest.fn();
    let requestCriticalFieldsUpdate = jest.fn();

    let wrapper = null;
    let datepickerInstance = null;
    let roleSuggestInstance = null;
    let roleOnDutySuggestInstance = null;

    inherit.self(TaDatepicker, {
        willInit() {
            this.__base(...arguments);
            datepickerInstance = this;
        },
    });

    inherit.self(TaSuggest, {
        willInit() {
            this.__base(...arguments);

            if (!roleSuggestInstance) {
                roleSuggestInstance = this;
            }

            if (roleSuggestInstance !== this && !roleOnDutySuggestInstance) {
                roleOnDutySuggestInstance = this;
            }
        },
    });

    beforeEach(() => {
        const data =
        {
            name: 'test',
            slug: '',
            role: null,
            roleOnDuty: null,
            personsCount: 2,
            considerOtherSchedules: true,
            duration: 2,
            dutyOnHolidays: false,
            dutyOnWeekends: false,
            startDate: new Date('2019-02-03'),
            needOrder: false,
        };

        wrapper = mount(
            <_Internal_SettingsContainer
                dutySchedules={{
                    single: data,
                    clientData: data,
                }}
                dutyPersons={{ persons: {}, schedules: {}, rolesErrors: {}, schedulesErrors: {} }}
                onCancel={onCancel}
                onSubmit={jest.fn()}
                service={{ id: 42 }}
                upsertDutySchedule={jest.fn()}
                resetDutySchedulesErrors={jest.fn()}
                resetDutySchedulesClientData={jest.fn()}
                getPersons={getPersons}
                getPersonsBySchedule={jest.fn()}
                requestCriticalFieldsUpdate={requestCriticalFieldsUpdate}
                updateDutySchedule={jest.fn()}
                updateServiceOptions={jest.fn()}
                deleteDutySchedule={jest.fn()}
                scheduleKey="123"
            />,
        );
    });

    afterEach(() => {
        wrapper.unmount();
        wrapper = null;
        datepickerInstance = null;
        roleSuggestInstance = null;
        roleOnDutySuggestInstance = null;
    });

    describe('Should handle fields change', () => {
        it('Name change', () => {
            const scheduleNameCtrl = wrapper.find('.DutySchedulesSettings__name').at(0).find('.textinput__control');
            scheduleNameCtrl.simulate('change', { target: { value: 'different value' } });
            expect(wrapper.state('schedule').name).toBe('different value');
        });

        it('Slug change', () => {
            const scheduleSlugCtrl = wrapper.find('.DutySchedulesSettings__slug').at(0).find('.textinput__control');
            scheduleSlugCtrl.simulate('change', { target: { value: 'new_slug' } });
            expect(wrapper.state('schedule').slug).toBe('new_slug');
        });

        it('Role change', () => {
            roleSuggestInstance._changeValues([() => [{}, { id: 19, name: { ru: 'Какая-то роль' } }]]);
            expect(wrapper.state('schedule').role).toEqual({ id: 19, name: { ru: 'Какая-то роль' } });
        });

        it('Role on duty change', () => {
            roleOnDutySuggestInstance._changeValues([() => [{}, { id: 19, name: { ru: 'Какая-то роль' } }]]);
            expect(wrapper.state('schedule').roleOnDuty).toEqual(19);
        });

        it('Persons count change', () => {
            const schedulePersonsCountCtrl = wrapper
                .find('.DutySchedulesSettings__persons-count')
                .at(0)
                .find('.textinput__control');
            schedulePersonsCountCtrl.simulate('change', { target: { value: 42 } });
            expect(wrapper.state('schedule').personsCount).toBe(42);
        });

        it('Consider other schedules change', () => {
            expect(wrapper.state('schedule').considerOtherSchedules).toBe(true);
            const considerOtherSchedulesCtrl = wrapper
                .find('.DutySchedulesSettings__consider-other-schedules')
                .at(0)
                .find('.checkbox__control');
            considerOtherSchedulesCtrl.simulate('change', { target: { checked: true } });
            expect(wrapper.state('schedule').considerOtherSchedules).toBe(false);
        });

        it('Start date change', () => {
            datepickerInstance._onDateSelect({ date: new Date('2019-02-01') });
            expect(wrapper.state('schedule').startDate).toEqual(new Date('2019-02-01'));
        });

        it('Duration change', () => {
            const durationCtrl = wrapper.find('.DutySchedulesSettings__duration').at(0).find('.textinput__control');
            durationCtrl.simulate('change', { target: { value: 20 } });
            expect(wrapper.state('schedule').duration).toBe(20);
        });

        it('duty on holidays toggle', () => {
            expect(wrapper.state('schedule').dutyOnHolidays).toBe(false);
            const dutyOnHolidaysCtrl = wrapper.find('.DutySchedulesSettings__duty-on-holidays').at(0).find('.checkbox__control');
            dutyOnHolidaysCtrl.simulate('change', { target: { checked: true } });
            expect(wrapper.state('schedule').dutyOnHolidays).toBe(true);
        });

        it('Duty on weekends toggle', () => {
            expect(wrapper.state('schedule').dutyOnWeekends).toBe(false);
            const dutyOnWeekendsCtrl = wrapper.find('.DutySchedulesSettings__duty-on-weekends').at(0).find('.checkbox__control');
            dutyOnWeekendsCtrl.simulate('change', { target: { checked: true } });
            expect(wrapper.state('schedule').dutyOnWeekends).toBe(true);
        });

        it('NeedOrder change', () => {
            expect(wrapper.state('schedule').needOrder).toBe(false);
            const needOrderCtrl1 = wrapper.find('.DutySchedulesSettings__need-order').at(0).find('.checkbox__control');
            needOrderCtrl1.simulate('change', { target: { checked: true } });
            expect(wrapper.state('schedule').needOrder).toBe(true);

            expect(getPersons).toHaveBeenCalled();
        });
    });

    describe('Should handle side effects', () => {
        beforeAll(() => {
            // нужен уникальный мок, потому что проверяем отложенную проверку,
            // она отложенно вызовется во всех остальных кейсах тоже и сломает счётчик
            requestCriticalFieldsUpdate = jest.fn();
        });

        it('Should request critical fields status after update', done => {
            const initialCalls = requestCriticalFieldsUpdate.mock.calls.length;

            const durationCtrl = wrapper.find('.DutySchedulesSettings__duration').at(0).find('.textinput__control');
            durationCtrl.simulate('change', { target: { value: 42 } });

            expect(requestCriticalFieldsUpdate).toHaveBeenCalledTimes(initialCalls);

            // fake timers не работают из коробки с lodash debounce, нужно мокать чуть сложнее
            // https://github.com/facebook/jest/issues/3465#issuecomment-504908570
            setTimeout(() => {
                expect(requestCriticalFieldsUpdate).toHaveBeenCalledTimes(initialCalls + 1);
                done();
            }, 1000);
        });
    });
});

describe('Should update persons', () => {
    const getPersons = jest.fn();
    const newPersons = {
        ['default']: [{
            login: 'user1',
            name: { ru: 'Имя1 Фамилия1', en: 'Name1 Surname1' },
        }],
    };
    const newPersonsBySchedule = {
        ['35']: {
            orders: [{
                login: 'user2',
                name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
            }],
            missingOrders: [{
                login: 'user3',
                name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' },
            }],
            activeOrders: [],
        },
    };

    function createWrapper(data) {
        return mount(
            <_Internal_SettingsContainer
                dutySchedules={{
                    single: data,
                    clientData: data,
                }}
                dutyPersons={{
                    persons: newPersons,
                    schedules: newPersonsBySchedule,
                    rolesErrors: {},
                    schedulesErrors: {},
                }}
                onCancel={jest.fn()}
                onSubmit={jest.fn()}
                service={{ id: 42 }}
                upsertDutySchedule={jest.fn()}
                resetDutySchedulesErrors={jest.fn()}
                resetDutySchedulesClientData={jest.fn()}
                getPersons={getPersons}
                getPersonsBySchedule={jest.fn()}
                updateDutySchedule={jest.fn()}
                updateServiceOptions={jest.fn()}
                deleteDutySchedule={jest.fn()}
                scheduleKey="123"
            />,
        );
    }

    it('Update persons without schedules', () => {
        const wrapper = createWrapper({
            name: 'test',
            role: null,
            personsCount: 2,
            considerOtherSchedules: true,
            duration: 2,
            dutyOnHolidays: false,
            dutyOnWeekends: false,
            startDate: new Date('2019-02-03'),
            needOrder: true,
        });
        wrapper.instance()._updatePersons(newPersons);

        expect(wrapper.state('schedule').orders).toEqual([{
            login: 'user1',
            name: { ru: 'Имя1 Фамилия1', en: 'Name1 Surname1' },
        }]);

        wrapper.unmount();
    });

    it('Update persons with schedules', () => {
        const wrapper = createWrapper({
            name: 'release',
            slug: 'release',
            id: 35,
            role: null,
            personsCount: 1,
            considerOtherSchedules: false,
            duration: 5,
            dutyOnHolidays: true,
            dutyOnWeekends: true,
            startDate: new Date('2019-01-01'),
            needOrder: true,
        });
        wrapper.instance()._updatePersonsBySchedule(newPersonsBySchedule);

        expect(wrapper.state('schedule').orders).toEqual([{
            login: 'user2',
            name: { ru: 'Имя2 Фамилия2', en: 'Name2 Surname2' },
        }]);
        expect(wrapper.state('schedule').missingOrders).toEqual([{
            login: 'user3',
            name: { ru: 'Имя3 Фамилия3', en: 'Name3 Surname3' },
        }]);

        wrapper.unmount();
    });
});
