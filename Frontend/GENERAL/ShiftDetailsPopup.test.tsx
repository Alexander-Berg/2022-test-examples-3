import React from 'react';
import { shallow } from 'enzyme';
import { ShiftDetailsPopup } from './ShiftDetailsPopup';
import type { ShiftDetails, User } from '../../Calendar/Calendar.types';

function doNothing() { }

const defaultProps = {
    canApproveOwnDutySetting: false,
    isApproved: false,
    dutyOnHolidays: false,
    dutyOnWeekends: false,
    requesterInDuty: false,
    user: {} as User,
    replacements: [],
    scheduleId: 1,
};

describe('Should render shift-details-popup', () => {
    it('With no shift details provided', () => {
        const wrapper = shallow(
            <ShiftDetailsPopup
                shiftDetails={{}}
                storedApprovedFlags={{}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('With a single shift details object provided', () => {
        const wrapper = shallow(
            <ShiftDetailsPopup
                shiftDetails={{
                    [1]: {
                        ...defaultProps,
                        isVisible: true,
                        canEditDutySettings: true,
                        person: {
                            name: 'John Doe',
                            login: 'john.doe',
                            vteams: [],
                        },
                        startDate: new Date(2019, 1, 1),
                        endDate: new Date(2019, 1, 15),
                        onMouseEnter: doNothing,
                        onMouseLeave: doNothing,
                        onOpenShiftEditClick: doNothing,
                        getRef: doNothing as ShiftDetails['getRef'],
                    },
                }}
                storedApprovedFlags={{}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('With a single non-visible shift details object provided', () => {
        const wrapper = shallow(
            <ShiftDetailsPopup
                shiftDetails={{
                    [1]: {
                        ...defaultProps,
                        isVisible: false,
                        canEditDutySettings: true,
                        person: {
                            name: 'John Doe',
                            login: 'john.doe',
                            vteams: [],
                        },
                        startDate: new Date(2019, 1, 1),
                        endDate: new Date(2019, 1, 15),
                        onMouseEnter: doNothing,
                        onMouseLeave: doNothing,
                        onOpenShiftEditClick: doNothing,
                        getRef: doNothing as ShiftDetails['getRef'],
                    },
                }}
                storedApprovedFlags={{}}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('With a couple of shift details objects provided', () => {
        const wrapper = shallow(
            <div>
                <ShiftDetailsPopup
                    shiftDetails={{
                        [1]: {
                            ...defaultProps,
                            isVisible: true,
                            canEditDutySettings: true,
                            person: {
                                name: 'John Doe',
                                login: 'john.doe',
                                vteams: [],
                            },
                            startDate: new Date(2019, 1, 1),
                            endDate: new Date(2019, 1, 15),
                            onMouseEnter: doNothing,
                            onMouseLeave: doNothing,
                            onOpenShiftEditClick: doNothing,
                            getRef: doNothing as ShiftDetails['getRef'],
                        },
                        [2]: {
                            ...defaultProps,
                            isVisible: true,
                            canEditDutySettings: true,
                            person: {
                                name: 'Dave Due',
                                login: 'dave.due',
                                vteams: [],
                            },
                            startDate: new Date(2019, 1, 1),
                            endDate: new Date(2019, 1, 15),
                            onMouseEnter: doNothing,
                            onMouseLeave: doNothing,
                            onOpenShiftEditClick: doNothing,
                            getRef: doNothing as ShiftDetails['getRef'],
                        },
                    }}
                    storedApprovedFlags={{}}
                />
            </div>,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
