import React from 'react';
import { shallow } from 'enzyme';

import { Duty } from './Duty';

describe('Duty', () => {
    it('Should render duty page', () => {
        const wrapper = shallow(
            <Duty
                onSettingsClose={() => {
                }}
                settings={{
                    collection: [{
                        name: 'release',
                        role: null,
                        personsCount: 1,
                        considerOtherSchedules: true,
                        duration: 5,
                        dutyOnHolidays: true,
                        dutyOnWeekends: true,
                        startDate: new Date('2019-01-01'),
                    }],
                    loading: false,
                }}
                canEditDutySettings
                isSettingsOpen
                service={{ id: 42 }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render duty page without popups', () => {
        const wrapper = shallow(
            <Duty
                onSettingsClose={() => {
                }}
                settings={{
                    loading: false,
                    collection: [{
                        name: 'release',
                        role: null,
                        personsCount: 1,
                        considerOtherSchedules: true,
                        duration: 5,
                        dutyOnHolidays: true,
                        dutyOnWeekends: true,
                        startDate: new Date('2019-01-01'),
                    }],
                }}
                canEditDutySettings
                service={{ id: 42 }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render duty page without popups and modal', () => {
        const wrapper = shallow(
            <Duty
                onSettingsClose={() => {
                }}
                settings={{
                    loading: false,
                    collection: [{
                        name: 'release',
                        role: null,
                        personsCount: 1,
                        considerOtherSchedules: true,
                        duration: 5,
                        dutyOnHolidays: true,
                        dutyOnWeekends: true,
                        startDate: new Date('2019-01-01'),
                    }],
                }}
                service={{ id: 42 }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
