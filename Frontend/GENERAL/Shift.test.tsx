import React from 'react';
import { shallow } from 'enzyme';

import { Shift } from './Shift';

describe('Shift', () => {
    it('Should render schedule edit shift item', () => {
        const wrapper = shallow(
            <Shift
                id={42}
                person
                start={new Date(Date.UTC(2010, 1, 1))}
                end={new Date(Date.UTC(2010, 1, 10))}
                problemsCount={0}
                onOpenShiftEditClick={() => { }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render schedule edit shift item with problems', () => {
        const wrapper = shallow(
            <Shift
                id={42}
                start={new Date(Date.UTC(2010, 1, 1))}
                end={new Date(Date.UTC(2010, 1, 10))}
                problemsCount={1}
                onOpenShiftEditClick={() => { }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
