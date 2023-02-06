import React from 'react';
import { shallow } from 'enzyme';
import { Shift } from './Shift';
import { Person } from '../../redux/DutyShifts.types';

describe('Should render DutyShift', () => {
    it('In pending status', () => {
        const wrapper = shallow(
            <Shift
                label="Hello world"
                replacements={[]}
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('In approved status', () => {
        const wrapper = shallow(
            <Shift
                isApproved
                label="Hello world"
                replacements={[]}
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('With temporary replacements', () => {
        const wrapper = shallow(
            <Shift
                isApproved
                replacements={[{
                    id: 1,
                    startDate: new Date(Date.UTC(2019, 1, 1)),
                    endDate: new Date(Date.UTC(2019, 1, 5)),
                    isDeleted: true,
                    start: 0,
                    length: 1,
                    person: {
                        login: 'login0@',
                        name: { ru: 'Имя0', en: 'Name0' },
                    } as Person,
                }]}
                label="Hello world"
                onOpenShiftEditClick={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
