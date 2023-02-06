import React from 'react';
import { shallow } from 'enzyme';
import { DateTime } from 'luxon';
import { ShiftEdit } from './ShiftEdit';
import { EAbsenceType } from '../../redux/DutySchedules.types';
import type { ReplaceForValidation } from './ShiftEdit.types';

describe('Should render shift edit form', () => {
    it('without person', () => {
        const wrapper = shallow(
            <ShiftEdit
                start={new Date(2019, 1, 1)}
                end={new Date(2010, 1, 5)}
                onCancel={() => null}
                onPersonChange={() => null}
                absences={[]}
                replaces={[]}
                addReplace={() => null}
                deleteReplace={() => null}
                onReplacePersonChange={() => null}
                onReplaceStartChange={() => null}
                onReplaceEndChange={() => null}
                shiftChangeTime={null}
                onSubmit={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in disabled state', () => {
        const wrapper = shallow(
            <ShiftEdit
                start={new Date(2019, 1, 1)}
                end={new Date(2010, 1, 5)}
                onCancel={() => null}
                onPersonChange={() => null}
                disabled
                absences={[]}
                replaces={[]}
                addReplace={() => null}
                deleteReplace={() => null}
                onReplacePersonChange={() => null}
                onReplaceStartChange={() => null}
                onReplaceEndChange={() => null}
                shiftChangeTime={null}
                onSubmit={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in loading state', () => {
        const wrapper = shallow(
            <ShiftEdit
                start={new Date(2019, 1, 1)}
                end={new Date(2010, 1, 5)}
                onCancel={() => null}
                onPersonChange={() => null}
                disabled
                absences={[]}
                replaces={[]}
                addReplace={() => null}
                deleteReplace={() => null}
                onReplacePersonChange={() => null}
                onReplaceStartChange={() => null}
                onReplaceEndChange={() => null}
                shiftChangeTime={null}
                onSubmit={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with absences', () => {
        const wrapper = shallow(
            <ShiftEdit
                start={new Date(2019, 1, 1)}
                end={new Date(2010, 1, 5)}
                onCancel={() => null}
                onPersonChange={() => null}
                absences={[
                    {
                        id: 1,
                        type: EAbsenceType.Vacation,
                        start: DateTime.fromJSDate(new Date(2019, 1, 1)),
                        end: DateTime.fromJSDate(new Date(2019, 1, 10)),
                        fullDay: true,
                        workInAbsence: false,
                        person: { login: 'hey' },
                    },
                ]}
                replaces={[]}
                addReplace={() => null}
                deleteReplace={() => null}
                onReplacePersonChange={() => null}
                onReplaceStartChange={() => null}
                onReplaceEndChange={() => null}
                shiftChangeTime={null}
                onSubmit={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with replaces', () => {
        const wrapper = shallow(
            <ShiftEdit
                start={new Date(2019, 1, 1)}
                end={new Date(2010, 1, 5)}
                onCancel={() => null}
                onPersonChange={() => null}
                absences={[]}
                replaces={[{
                    id: 101,
                    personId: 201,
                    personLogin: 'person',
                    start: new Date(2019, 1, 1),
                    end: new Date(2019, 1, 10),
                } as ReplaceForValidation]}
                addReplace={() => null}
                deleteReplace={() => null}
                onReplacePersonChange={() => null}
                onReplaceStartChange={() => null}
                onReplaceEndChange={() => null}
                shiftChangeTime={null}
                onSubmit={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with messages about dutyOnHolidays && dutyOnWeekends', () => {
        const wrapper = shallow(
            <ShiftEdit
                start={new Date(2019, 1, 1)}
                end={new Date(2010, 1, 5)}
                onCancel={() => null}
                onPersonChange={() => null}
                absences={[]}
                replaces={[]}
                addReplace={() => null}
                deleteReplace={() => null}
                onReplacePersonChange={() => null}
                onReplaceStartChange={() => null}
                onReplaceEndChange={() => null}
                shiftChangeTime={null}
                onSubmit={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
