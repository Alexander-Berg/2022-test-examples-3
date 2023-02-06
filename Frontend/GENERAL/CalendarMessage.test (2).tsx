import React from 'react';
import { shallow } from 'enzyme';

import { CalendarMessage } from './CalendarMessage';

describe('CalendarMessage', () => {
    it('Should render CalendarMessage', () => {
        const wrapper = shallow(
            <CalendarMessage
                header="HEADER"
            >
                <div>1</div>
                <div>2</div>
            </CalendarMessage>,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });
});
