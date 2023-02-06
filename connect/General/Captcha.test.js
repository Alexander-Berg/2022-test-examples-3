import React from 'react';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';
import Captcha from './Captcha';

configure({ adapter: new Adapter() });

describe('client/components/Captcha', () => {
    it('Should render Captcha', () => {
        const wrapper = mount(
            <Captcha
                src="//src.src"
                text="text"
                onChange={jest.fn()}
                onRefresh={jest.fn()}
                loading={false}
                labels={{
                    refresh: 'refresh',
                    label: 'label',
                    placeholder: 'placeholder',
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render loading Captcha', () => {
        const wrapper = mount(
            <Captcha
                src="//src.src"
                text="text"
                onChange={jest.fn()}
                onRefresh={jest.fn()}
                loading
                labels={{
                    refresh: 'refresh',
                    label: 'label',
                    placeholder: 'placeholder',
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should call props.onChange on change', () => {
        const onChange = jest.fn();
        const text = 'foo';

        const wrapper = mount(
            <Captcha
                src="//src.src"
                text={text}
                onChange={onChange}
                onRefresh={jest.fn()}
                loading={false}
                labels={{
                    refresh: 'refresh',
                    label: 'label',
                    placeholder: 'placeholder',
                }}
            />
        );

        wrapper.find('.textinput__control').simulate('change');
        expect(onChange).toHaveBeenCalledWith(text);

        wrapper.unmount();
    });

    it('Should call props.onRefresh on refresh click', () => {
        const onRefresh = jest.fn();

        const wrapper = mount(
            <Captcha
                src="//src.src"
                text="foo"
                onRefresh={onRefresh}
                onChange={jest.fn()}
                loading={false}
                labels={{
                    refresh: 'refresh',
                    label: 'label',
                    placeholder: 'placeholder',
                }}
            />
        );

        wrapper
            .find('.captcha__controls')
            .find('.link')
            .simulate('click');
        expect(onRefresh).toHaveBeenCalled();

        wrapper.unmount();
    });
});
