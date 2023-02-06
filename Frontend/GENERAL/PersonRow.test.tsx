import React from 'react';
import { shallow } from 'enzyme';
import { PersonRow } from './PersonRow';

describe('Should render person row', () => {
    it('without person', () => {
        const wrapper = shallow(
            <PersonRow
                onPersonChange={() => null}
                start={null}
                end={null}
                name="person"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with dates', () => {
        const wrapper = shallow(
            <PersonRow
                onPersonChange={() => null}
                shiftStart={new Date(Date.UTC(2018, 11, 25))}
                shiftEnd={new Date(Date.UTC(2019, 0, 20))}
                start={new Date(Date.UTC(2019, 0, 1))}
                end={new Date(Date.UTC(2019, 0, 10))}
                name="person"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with ability to edit all', () => {
        const wrapper = shallow(
            <PersonRow
                onPersonChange={() => null}
                start={null}
                end={null}
                canEditDates
                canDelete
                name="person"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('with invalid dates', () => {
        const wrapper = shallow(
            <PersonRow
                onPersonChange={() => null}
                start={null}
                end={null}
                invalidStart
                invalidEnd
                name="person"
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
