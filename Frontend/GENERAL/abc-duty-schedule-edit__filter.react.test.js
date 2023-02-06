import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyScheduleEdit__Filter from 'b:abc-duty-schedule-edit e:filter';

describe('AbcDutyScheduleEdit__Filter', () => {
    it('Should render schedule edit filter', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__Filter>
                hello world
            </AbcDutyScheduleEdit__Filter>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
