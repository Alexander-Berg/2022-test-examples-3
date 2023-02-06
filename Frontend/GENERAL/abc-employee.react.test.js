import React from 'react';
import { shallow } from 'enzyme';

import AbcEmployee from 'b:abc-employee m:no-person=yes';

describe('AbcEmployee', () => {
    it('Should render employee with default params', () => {
        const wrapper = shallow(
            <AbcEmployee
                login="robot-serptools"
                name="WALL-E"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render large employee with caption and counter', () => {
        const wrapper = shallow(
            <AbcEmployee
                login="robot-serptools"
                name="WALL-E"
                caption="the most cute robot"
                counter={42}
                size="l"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render large employee with no person', () => {
        const wrapper = shallow(
            <AbcEmployee
                login=""
                name="no person on duty"
                noPerson="yes"
                caption={null}
                counter={42}
                size="l"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
