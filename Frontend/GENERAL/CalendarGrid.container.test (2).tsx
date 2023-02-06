/* eslint-disable @typescript-eslint/no-non-null-assertion */
import React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import { CalendarGridContainer } from './CalendarGrid.container';
import type { ShiftDetails, User } from '../Calendar/Calendar.types';
import { EScheduleState } from '~/src/features/Duty2/redux/DutySchedules.types';

function wait(delay: number) {
    return new Promise(function(resolve) {
        setTimeout(resolve, delay);
    });
}

function doNothing() { }

describe('State is changing correctly', () => {
    const forgetDetailsDelay = 100;
    const DUTY_DETAILS: Record<number, ShiftDetails> = {
        [1]: {
            canEditDutySettings: true,
            onMouseEnter: doNothing,
            onMouseLeave: doNothing,
            onOpenShiftEditClick: doNothing,
            getRef: jest.fn(),
            user: {} as User,
        },
        [2]: {
            canEditDutySettings: true,
            onMouseEnter: doNothing,
            onMouseLeave: doNothing,
            onOpenShiftEditClick: doNothing,
            getRef: jest.fn(),
            user: {} as User,
        },
    };

    let wrapper: ShallowWrapper<CalendarGridContainer['props'], CalendarGridContainer['state'], CalendarGridContainer> | null = null;

    beforeEach(() => {
        wrapper = shallow(
            <CalendarGridContainer
                data={{
                    schedule: {
                        id: 100,
                        name: 'Schedule Name',
                        revision: null,
                        serviceId: 1,
                        state: EScheduleState.active,
                        recalculationInProcess: false,
                        persons: {},
                        daysStatus: {},
                    },
                    daysList: [],
                    personsCount: 0,
                    holidaysList: [],
                    absences: {},
                }}
                serviceId={1}
                user={{} as User}
                forgetDetailsDelay={forgetDetailsDelay}
                deleteScheduleLoading={false}
                onDeleteSchedule={jest.fn()}
                onOpenShiftEditClick={jest.fn()}
                start={new Date(2021, 1, 1)}
                scale="day"
            />,
        );
    });

    afterEach(() => {
        wrapper!.unmount();
    });

    describe('onShiftHoverChange', () => {
        it('Should store hoveredShiftId if isHovered', () => {
            const shiftId = 1;

            wrapper!.instance().onShiftHoverChange(true, shiftId);
            expect(wrapper!.state('hoveredShiftId')).toBe(shiftId);
        });

        it('Should store null if not isHovered', () => {
            wrapper!.instance().onShiftHoverChange(false, 1);
            expect(wrapper!.state('hoveredShiftId')).toBeNull();
        });
    });

    describe('showDutyDetails and hideDutyDetails', () => {
        it('Should add duty details object to the state, then hide and remove it after timeout', async() => {
            const firstShiftId = 1;

            wrapper!.instance().showDutyDetails(firstShiftId, DUTY_DETAILS[firstShiftId]);
            expect(wrapper!.state('shiftDetails')[firstShiftId]).toMatchObject(DUTY_DETAILS[firstShiftId]);
            expect(wrapper!.state('shiftDetails')[firstShiftId]?.isVisible).toBeTruthy();

            const secondShiftId = 2;

            wrapper!.instance().showDutyDetails(secondShiftId, DUTY_DETAILS[secondShiftId]);
            expect(wrapper!.state('shiftDetails')[secondShiftId]).toMatchObject(DUTY_DETAILS[secondShiftId]);
            expect(wrapper!.state('shiftDetails')[secondShiftId]?.isVisible).toBeTruthy();

            wrapper!.instance().hideDutyDetails(firstShiftId);
            expect(wrapper!.state('shiftDetails')[firstShiftId]?.isVisible).toBeFalsy();

            await wait(forgetDetailsDelay);
            expect(wrapper!.state('shiftDetails')[firstShiftId]).toBeFalsy();
        });
    });
});
