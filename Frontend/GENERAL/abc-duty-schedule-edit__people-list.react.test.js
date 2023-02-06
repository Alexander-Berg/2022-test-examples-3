import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyScheduleEdit__PeopleList from 'b:abc-duty-schedule-edit e:people-list';

describe('AbcDutyScheduleEdit__PeopleList', () => {
    it('Should render list of employees', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__PeopleList
                people={{
                    login1: {
                        name: { ru: 'Ваня' },
                        counter: 42
                    },
                    login2: {
                        name: { ru: 'Петя' },
                        counter: 0
                    }
                }}
                selected="login1"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render empty list', () => {
        const wrapper = shallow(
            <AbcDutyScheduleEdit__PeopleList />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
