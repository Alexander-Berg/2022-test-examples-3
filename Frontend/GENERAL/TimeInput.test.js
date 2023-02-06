import React from 'react';
import { render, mount } from 'enzyme';

import { TimeInput } from './TimeInput';

const hoursControlClassname = '.TimeInput-Hours .Textinput-Control';
const minutesControlClassname = '.TimeInput-Minutes .Textinput-Control';

describe('TimeInput ', () => {
    describe('Check snapshots', () => {
        it('default', () => {
            const wrapper = render(
                <TimeInput value="16:42" />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('disabled', () => {
            const wrapper = render(
                <TimeInput value="16:42" disabled />
            );

            expect(wrapper).toMatchSnapshot();
        });

        it('with inputId', () => {
            const wrapper = render(
                <TimeInput value="16:42" inputId="some_id" />
            );

            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('Check onChange calls', () => {
        let onChange;
        let wrapper;

        beforeEach(() => {
            onChange = jest.fn();

            wrapper = mount(
                <TimeInput value="05:06" onChange={onChange} />
            );
        });

        afterEach(() => {
            jest.clearAllMocks();
            wrapper.unmount();
        });

        it('Should fire onChange', () => {
            const hoursInput = wrapper.find(hoursControlClassname);
            const minutesInput = wrapper.find(minutesControlClassname);

            hoursInput.simulate('change', { target: { value: '12' } });
            expect(onChange).toHaveBeenCalledWith('12:06');

            minutesInput.simulate('change', { target: { value: '30' } });
            expect(onChange).toHaveBeenCalledWith('05:30');
        });

        it('Should loop hours and minutes', () => {
            const hoursInput = wrapper.find(hoursControlClassname);
            const minutesInput = wrapper.find(minutesControlClassname);

            hoursInput.simulate('change', { target: { value: '-1' } });
            expect(onChange).toHaveBeenCalledWith('23:06');

            hoursInput.simulate('change', { target: { value: '24' } });
            expect(onChange).toHaveBeenCalledWith('00:06');

            minutesInput.simulate('change', { target: { value: '-1' } });
            expect(onChange).toHaveBeenCalledWith('05:59');

            minutesInput.simulate('change', { target: { value: '60' } });
            expect(onChange).toHaveBeenCalledWith('05:00');
        });

        it('Should set hours to 23 when overflown', () => {
            const hoursInput = wrapper.find(hoursControlClassname);

            hoursInput.simulate('change', { target: { value: '25' } });

            expect(onChange).toHaveBeenCalledWith('23:06');
        });

        it('Should set hours to 0 when underflown', () => {
            const hoursInput = wrapper.find(hoursControlClassname);

            hoursInput.simulate('change', { target: { value: '-2' } });

            expect(onChange).toHaveBeenCalledWith('00:06');
        });

        it('Should set minutes to 59 when overflown', () => {
            const minutesInput = wrapper.find(minutesControlClassname);

            minutesInput.simulate('change', { target: { value: '61' } });

            expect(onChange).toHaveBeenCalledWith('05:59');
        });

        it('Should set minutes to 0 when underflown', () => {
            const minutesInput = wrapper.find(minutesControlClassname);

            minutesInput.simulate('change', { target: { value: '-2' } });

            expect(onChange).toHaveBeenCalledWith('05:00');
        });

        it('Should keep the last 2 digits', () => {
            const hoursInput = wrapper.find(hoursControlClassname);
            const minutesInput = wrapper.find(minutesControlClassname);

            hoursInput.simulate('change', { target: { value: '-12' } });
            expect(onChange).toHaveBeenCalledWith('12:06');

            hoursInput.simulate('change', { target: { value: '123' } });
            expect(onChange).toHaveBeenCalledWith('23:06');

            minutesInput.simulate('change', { target: { value: '-12' } });
            expect(onChange).toHaveBeenCalledWith('05:12');

            minutesInput.simulate('change', { target: { value: '123' } });
            expect(onChange).toHaveBeenCalledWith('05:23');
        });
    });

    describe('Check props', () => {
        it('Should update value from props', () => {
            const wrapper = mount(
                <TimeInput value="05:06" />
            );

            expect(wrapper.find(hoursControlClassname).instance().value).toBe('05');
            expect(wrapper.find(minutesControlClassname).instance().value).toBe('06');

            wrapper.setProps({ value: '12:34' });

            expect(wrapper.find(hoursControlClassname).instance().value).toBe('12');
            expect(wrapper.find(minutesControlClassname).instance().value).toBe('34');

            wrapper.unmount();
        });
    });
});
