import React from 'react';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';
import InputItem from './InputItem';

configure({ adapter: new Adapter() });

describe('client/components/InviteForm/components/InputItem', () => {
    it('Should render Input Item', () => {
        const wrapper = mount(
            <InputItem
                onRemove={jest.fn()}
                content="foo"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
