import React from 'react';
import { render } from 'enzyme';
import DatePicker from './DatePicker';

describe('Should render InlineDatePicker', () => {
    it('disable picker without date', () => {
        const wrapper = render(
            <DatePicker
                onChange={jest.fn()}
                disabled
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('disable picker with date', () => {
        const wrapper = render(
            <DatePicker
                onChange={jest.fn()}
                date={new Date(Date.UTC(2019, 8, 26))}
                disabled
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('enable picker with date', () => {
        const wrapper = render(
            <DatePicker
                onChange={jest.fn()}
                date={new Date(Date.UTC(2019, 8, 26))}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('enable picker without date', () => {
        const wrapper = render(
            <DatePicker
                onChange={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
