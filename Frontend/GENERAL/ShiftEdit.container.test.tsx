/* eslint-disable @typescript-eslint/no-non-null-assertion */
import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
// @ts-ignore
import inherit from 'inherit';
import 'babel-polyfill';
import { DateTime } from 'luxon';

import { TaSuggest } from './ShiftEdit.legacy';

import { _Internal_ShiftEditContainer, _getShiftAbsences, _predictReplaceDates, ShiftEditContainerState } from './ShiftEdit.container';
import type { DutyAbsencesStore, ReplaceForValidation } from './ShiftEdit.types';
import type { DutyShiftsScope, Filters, Person } from '../../redux/DutyShifts.types';
import { Absence, AbsentPerson, EAbsenceType, Schedule } from '../../redux/DutySchedules.types';
import { EDataScope } from '~/src/abc/react/redux/types';

jest.mock('~/src/common/components/DateInterval/DateInterval');
jest.mock('~/src/common/components/DatePicker/DatePicker', () => () => '');

describe('Component abc-duty-shift-edit-container', () => {
    const dutyShifts: DutyShiftsScope = {
        loading: false,
        data: [{
            id: 101,
            person: {
                id: 201,
                login: 'person-1',
            } as Person,
            schedule: {
                id: 301,
            },
            replaces: [{
                id: 101,
                person: {
                    id: 202,
                    login: 'person-2',
                } as Person,
                schedule: {
                    id: 301,
                },
                start: '2019-01-21',
                end: '2019-01-22',
                start_datetime: '2019-01-21T00:00:00',
                end_datetime: '2019-01-23T00:00:00',
            }, {
                id: 500,
                person: {
                    id: 501,
                    login: 'person-3',
                } as Person,
                schedule: {
                    id: 301,
                },
                start: '2019-01-23',
                end: '2019-01-24',
                start_datetime: '2019-01-23T00:00:00',
                end_datetime: '2019-01-25T00:00:00',
                isDeleted: true,
            }],
            start_datetime: '2019-01-20T15:00+03:00',
            end_datetime: '2019-01-26T15:00+03:00',
            is_approved: false,
            start: '2019-01-20',
            end: '2019-01-26',
        }],
        error: null,
        filters: {} as Filters,
    };

    const dutyAbsences: DutyAbsencesStore = {
        data: [{
            id: 401,
            type: EAbsenceType.Vacation,
            start: DateTime.fromJSDate(new Date(2019, 0, 21)),
            end: DateTime.fromJSDate(new Date(2019, 0, 25)),
            person: { login: 'person-1' } as AbsentPerson,
            workInAbsence: false,
            fullDay: true,
        }],
    };

    const dutySchedulesData: Schedule[] = [{ id: 301 } as Schedule];

    it('should init properly', () => {
        inherit.self(TaSuggest, {
            _preload: () => ({
                done: () => null,
            }),
        });

        const wrapper = mount(
            <_Internal_ShiftEditContainer
                shiftId={101}
                dutyShifts={dutyShifts}
                dutyAbsences={dutyAbsences}
                dutySchedulesData={dutySchedulesData}
                patchDutyShift={() => null}
                onSave={() => null}
                onCancel={() => null}
                visible
                scope={EDataScope.Calendar}
            />,
        );

        expect(wrapper.state()).toEqual({
            personId: 201,
            personLogin: 'person-1',
            start: expect.toMatchDate(new Date(Date.UTC(2019, 0, 19, 21))),
            end: expect.toMatchDate(new Date(Date.UTC(2019, 0, 25, 21))),
            shiftChangeTime: expect.toMatchTime(new Date(0, 0, 0, 15)),
            absences: [{
                id: 401,
                type: 'vacation',
                start: expect.toMatchDate(new Date(2019, 0, 21)),
                end: expect.toMatchDate(new Date(2019, 0, 25)),
                person: { login: 'person-1' },
                workInAbsence: false,
                fullDay: true,
            }],
            originalShift: {
                id: 101,
                personId: 201,
                personLogin: 'person-1',
                start: expect.toMatchDate(new Date(Date.UTC(2019, 0, 19, 21))),
                end: expect.toMatchDate(new Date(Date.UTC(2019, 0, 25, 21))),
                shiftChangeTime: expect.toMatchTime(new Date(0, 0, 0, 15)),
                scheduleId: 301,
                replaces: [{
                    id: 101,
                    personId: 202,
                    personLogin: 'person-2',
                    scheduleId: 301,
                    start: expect.toMatchDate(new Date(2019, 0, 21)),
                    end: expect.toMatchDate(new Date(Date.UTC(2019, 0, 21, 21))),
                    shiftChangeTime: null,
                    isDeleted: false,
                }],
                isDeleted: false,
            },
            replaces: [{
                id: 101,
                personId: 202,
                personLogin: 'person-2',
                scheduleId: 301,
                start: expect.toMatchDate(new Date(2019, 0, 21)),
                end: expect.toMatchDate(new Date(Date.UTC(2019, 0, 21, 21))),
                shiftChangeTime: null,
                invalidEnd: false,
                invalidStart: false,
                isDeleted: false,
            }],
            replacesFilled: true,
            replacesIntersection: false,
            replacesOutOfRange: false,
            replacesInvalidDates: false,
        });

        wrapper.unmount();
    });

    it('should add replace', () => {
        inherit.self(TaSuggest, {
            _preload: () => ({
                done: () => null,
            }),
        });

        const wrapper = mount(
            <_Internal_ShiftEditContainer
                shiftId={101}
                dutyShifts={dutyShifts}
                dutyAbsences={dutyAbsences}
                dutySchedulesData={dutySchedulesData}
                patchDutyShift={() => null}
                onSave={() => null}
                onCancel={() => null}
                visible
                scope={EDataScope.Calendar}
            />,
        );

        expect(wrapper.find('.DutyShiftEdit-Replaces .DutyShiftEdit-PersonRow').length).toEqual(1);

        wrapper.find('.DutyShiftEdit__add-replace-button').simulate('click');

        expect(wrapper.find('.DutyShiftEdit-Replaces .DutyShiftEdit-PersonRow').length).toEqual(2);

        wrapper.unmount();
    });

    it('should delete replace', () => {
        inherit.self(TaSuggest, {
            _preload: () => ({
                done: () => null,
            }),
        });

        const wrapper = mount(
            <_Internal_ShiftEditContainer
                shiftId={101}
                dutyShifts={dutyShifts}
                dutyAbsences={dutyAbsences}
                dutySchedulesData={dutySchedulesData}
                patchDutyShift={() => null}
                onSave={() => null}
                onCancel={() => null}
                visible
                scope={EDataScope.Calendar}
            />,
        );

        expect(wrapper.find('.DutyShiftEdit-Replaces .DutyShiftEdit-PersonRow').length).toEqual(1);

        wrapper.find('.DutyShiftEdit-Replaces .DutyShiftEdit-PersonRow .DutyShiftEdit__delete-button:not(.DutyShiftEdit__delete-button_hidden)').simulate('click');

        expect(wrapper.find('.DutyShiftEdit-Replaces .DutyShiftEdit-PersonRow').length).toEqual(0);

        wrapper.unmount();
    });

    describe('should update original shift', () => {
        let wrapper: ReactWrapper | null = null;

        inherit.self(TaSuggest, {
            _preload: () => ({
                done: () => null,
            }),
        });

        beforeEach(() => {
            wrapper = mount(
                <_Internal_ShiftEditContainer
                    shiftId={101}
                    dutyShifts={dutyShifts}
                    dutyAbsences={dutyAbsences}
                    dutySchedulesData={dutySchedulesData}
                    patchDutyShift={() => null}
                    onSave={() => null}
                    onCancel={() => null}
                    visible
                    scope={EDataScope.Calendar}
                />,
            );
        });

        afterEach(() => {
            wrapper!.unmount();
        });

        it('upon shift id update', () => {
            expect(wrapper!.state('originalShift')).not.toBeNull();
            wrapper!.setProps({ shiftId: -1 });
            expect(wrapper!.state('originalShift')).toBeNull();
        });

        it('upon shift data update', () => {
            const modifiedDutyShifts = Object.assign({}, dutyShifts);
            modifiedDutyShifts.data[0].replaces[0].isDeleted = true;

            const expectedShift = Object.assign({}, wrapper!.state<ShiftEditContainerState>('originalShift'));
            expectedShift.replaces = [];

            wrapper!.setProps({ dutyShifts: modifiedDutyShifts });

            expect(wrapper!.state('originalShift')).toEqual(expectedShift);
        });
    });

    it('person on duty can not be deleted', () => {
        const wrapper = mount(
            <_Internal_ShiftEditContainer
                shiftId={101}
                dutyShifts={dutyShifts}
                dutyAbsences={dutyAbsences}
                dutySchedulesData={dutySchedulesData}
                patchDutyShift={() => null}
                onSave={() => null}
                onCancel={() => null}
                visible
                scope={EDataScope.Calendar}
            />,
        );

        wrapper.setState({ personId: null });
        // Если удалить дежурного, то произойдет изменение, но кнопка должна быть disabled, потому что нет дежурного
        expect(wrapper.find('.DutyShiftEdit__submit').prop('disabled')).toBeTruthy();
        wrapper.unmount();
    });

    it('person on duty can stay undefined', () => {
        // Можно сохранить дежурство без дежурного, когда его изначально нет
        const dutyShifts: DutyShiftsScope = {
            loading: false,
            data: [{
                id: 101,
                person: undefined,
                schedule: {
                    id: 301,
                },
                replaces: [{
                    id: 101,
                    person: {
                        id: 202,
                        login: 'person-2',
                    },
                    schedule: {
                        id: 301,
                    },
                    start: '2019-01-21',
                    end: '2019-01-22',
                    start_datetime: '2019-01-21T15:00+03:00',
                    end_datetime: '2019-01-22T15:00+03:00',
                }],
                is_approved: false,
                start: '2019-01-20',
                end: '2019-01-26',
                start_datetime: '2019-01-20T15:00+03:00',
                end_datetime: '2019-01-26T15:00+03:00',
            }],
            error: null,
            filters: {} as Filters,
        };

        const wrapper = mount(
            <_Internal_ShiftEditContainer
                shiftId={101}
                dutyShifts={dutyShifts}
                dutyAbsences={dutyAbsences}
                dutySchedulesData={dutySchedulesData}
                patchDutyShift={() => null}
                onSave={() => null}
                onCancel={() => null}
                visible
                scope={EDataScope.Calendar}
            />,
        );

        // Удаляем замену, чтобы сделать изменение и убедиться, что кнопка enabled
        wrapper.find('.DutyShiftEdit__delete-button:not(.DutyShiftEdit__delete-button_hidden)').simulate('click');
        expect(wrapper.find('.DutyShiftEdit__submit').prop('disabled')).toBeFalsy();
        wrapper.unmount();
    });
});

describe('Predict replace dates', () => {
    const start = new Date(2019, 7, 1);
    const end = new Date(2019, 7, 7);

    describe('with one absence', () => {
        const absences: Absence[] = [
            {
                start: DateTime.fromJSDate(new Date(2019, 6, 29)),
                end: DateTime.fromJSDate(new Date(2019, 7, 5)),
            } as Absence,
        ];

        it('without replaces', () => {
            const replaces: ReplaceForValidation[] = [];

            const expectedDates = {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take first interval', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 3),
                    end: new Date(2019, 7, 5),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 2),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take after one replace', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 4),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take after two replace', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 4),
                    end: new Date(2019, 7, 4),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 5),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take between two replace', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 5),
                    end: new Date(2019, 7, 5),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 4),
                end: new Date(2019, 7, 4),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('impossible to take', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 4),
                    end: new Date(2019, 7, 5),
                } as ReplaceForValidation,
            ];

            const expectedDates = undefined;

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('with almost unfilled replaces', () => {
            const replaces = [
                { start: new Date(2019, 7, 1) } as ReplaceForValidation,
                { end: new Date(2019, 7, 3) } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 2),
                end: new Date(2019, 7, 2),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('with unfilled replaces', () => {
            const replaces = [
                {} as ReplaceForValidation,
                {} as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('with outOfRange replaces', () => {
            const replaces = [
                {
                    start: new Date(2019, 6, 2),
                    end: new Date(2019, 6, 2),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 12),
                    end: new Date(2019, 7, 12),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 6, 29),
                    end: new Date(2019, 7, 2),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 3),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('with partially intersecting replaces', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 2),
                    end: new Date(2019, 7, 4),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 5),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('with intersecting replaces', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 2),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 4),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 5),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('with start > end replaces', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 2),
                    end: new Date(2019, 7, 1),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 4),
                    end: new Date(2019, 7, 2),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 5),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });
    });

    describe('with many absences', () => {
        const absences: Absence[] = [
            {
                start: DateTime.fromJSDate(new Date(2019, 6, 29)),
                end: DateTime.fromJSDate(new Date(2019, 7, 2)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 5)),
                end: DateTime.fromJSDate(new Date(2019, 7, 7)),
            } as Absence,
        ];

        it('without replaces', () => {
            const replaces: ReplaceForValidation[] = [];

            const expectedDates = {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 2),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take first interval in second absence', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 7),
                    end: new Date(2019, 7, 7),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 2),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 5),
                end: new Date(2019, 7, 6),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take after one replace in second absence', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 5),
                    end: new Date(2019, 7, 5),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 6),
                end: new Date(2019, 7, 7),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take after two replace in second absence', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 6),
                    end: new Date(2019, 7, 6),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 3),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 5),
                    end: new Date(2019, 7, 5),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 7),
                end: new Date(2019, 7, 7),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('take between two replace in second absence', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 7),
                    end: new Date(2019, 7, 7),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 2),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 5),
                    end: new Date(2019, 7, 5),
                } as ReplaceForValidation,
            ];

            const expectedDates = {
                start: new Date(2019, 7, 6),
                end: new Date(2019, 7, 6),
            };

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });

        it('impossible to take', () => {
            const replaces = [
                {
                    start: new Date(2019, 7, 1),
                    end: new Date(2019, 7, 2),
                } as ReplaceForValidation,
                {
                    start: new Date(2019, 7, 4),
                    end: new Date(2019, 7, 7),
                } as ReplaceForValidation,
            ];

            const expectedDates = undefined;

            const actualDate = _predictReplaceDates(start, end, absences, replaces);

            expect(actualDate).toEqual(expectedDates);
        });
    });
});

describe('Handle absence sequence', () => {
    const start = new Date(2019, 7, 1);
    const end = new Date(2019, 7, 10);

    it('with intersecting absences', () => {
        const absences: Absence[] = [
            {
                start: DateTime.fromJSDate(new Date(2019, 6, 29)),
                end: DateTime.fromJSDate(new Date(2019, 7, 2)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 2)),
                end: DateTime.fromJSDate(new Date(2019, 7, 3)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 3)),
                end: DateTime.fromJSDate(new Date(2019, 7, 5)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 8)),
                end: DateTime.fromJSDate(new Date(2019, 7, 8)),
            } as Absence,
        ];

        const expectedAbsences = [
            {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 5),
            },
            {
                start: new Date(2019, 7, 8),
                end: new Date(2019, 7, 8),
            },
        ];

        const actualAbsences = _getShiftAbsences(start, end, absences);

        expect(actualAbsences).toEqual(expectedAbsences);
    });

    it('with nested absences', () => {
        const absences: Absence[] = [
            {
                start: DateTime.fromJSDate(new Date(2019, 6, 29)),
                end: DateTime.fromJSDate(new Date(2019, 7, 8)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 1)),
                end: DateTime.fromJSDate(new Date(2019, 7, 3)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 5)),
                end: DateTime.fromJSDate(new Date(2019, 7, 6)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 10)),
                end: DateTime.fromJSDate(new Date(2019, 7, 10)),
            } as Absence,
        ];

        const expectedAbsences = [
            {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 8),
            },
            {
                start: new Date(2019, 7, 10),
                end: new Date(2019, 7, 10),
            },
        ];

        const actualAbsences = _getShiftAbsences(start, end, absences);

        expect(actualAbsences).toEqual(expectedAbsences);
    });

    it('with sequential absences', () => {
        const absences: Absence[] = [
            {
                start: DateTime.fromJSDate(new Date(2019, 6, 29)),
                end: DateTime.fromJSDate(new Date(2019, 7, 2)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 3)),
                end: DateTime.fromJSDate(new Date(2019, 7, 5)),
            } as Absence,
            {
                start: DateTime.fromJSDate(new Date(2019, 7, 7)),
                end: DateTime.fromJSDate(new Date(2019, 7, 9)),
            } as Absence,
        ];

        const expectedAbsences = [
            {
                start: new Date(2019, 7, 1),
                end: new Date(2019, 7, 5),
            },
            {
                start: new Date(2019, 7, 7),
                end: new Date(2019, 7, 9),
            },
        ];

        const actualAbsences = _getShiftAbsences(start, end, absences);

        expect(actualAbsences).toEqual(expectedAbsences);
    });
});
