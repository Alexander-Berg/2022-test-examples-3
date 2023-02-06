import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTag from 'b:abc-resource-tag m:role=add';

describe('AbcResourceTag', () => {
    it('Should render resource global tag with add control', () => {
        const wrapper = mount(
            <AbcResourceTag
                role="add"
                tag={{ name: { ru: 'tag_name' } }}
                onClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should call props.onClick on plus click', () => {
        const onClick = jest.fn();
        const tag = { name: { ru: 'tag_name' } };

        const wrapper = mount(
            <AbcResourceTag
                role="add"
                tag={tag}
                onClick={onClick}
            />
        );

        wrapper.find('.abc-resource-tag__add').simulate('click');
        expect(onClick).toHaveBeenCalledWith(tag);

        wrapper.unmount();
    });
});
