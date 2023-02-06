import React from 'react';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import AbcResourceTag from 'b:abc-resource-tag m:size=s';

configure({ adapter: new Adapter() });

describe('AbcResourceTag', () => {
    it('Should render resource global tag size s', () => {
        const wrapper = mount(
            <AbcResourceTag
                size="s"
                tag={{ name: { ru: 'tag_name' } }}
                onClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
