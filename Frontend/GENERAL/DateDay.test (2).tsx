import React from 'react';
import { shallow } from 'enzyme';

import { EScheduleStatus } from '~/src/features/Duty2/redux/DutySchedules.types';
import { DateDay } from './DateDay';

describe('DutyCalendarGrid-DateDay', () => {
    it('Should render date-day', () => {
        const wrapper = shallow(
            <DateDay
                status={EScheduleStatus.nothing}
                date={new Date(2019, 1, 2)}
                scale="day"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render date-day with month', () => {
        const wrapper = shallow(
            <DateDay
                status={EScheduleStatus.nothing}
                date={new Date(2019, 1, 1)}
                scale="day"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
