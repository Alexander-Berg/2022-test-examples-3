import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyScheduleEdit__Person from 'b:abc-duty-schedule-edit e:person';

describe('AbcDutyScheduleEdit__Person', () => {
    it('Should render schedule edit people list item', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__Person
                login="robot-serptools"
                name="WALL-E"
                counter={42}
                onClick={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render schedule edit people list item with no person', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__Person
                login=""
                name="no person on duty"
                counter={42}
                onClick={() => {}}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render selected schedule edit people list item', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__Person
                login="robot-serptools"
                name="WALL-E"
                counter={42}
                onClick={() => {}}
                selected
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
