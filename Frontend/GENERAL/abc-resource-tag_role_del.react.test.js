import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTag from 'b:abc-resource-tag m:role=del';

describe('AbcResourceTag', () => {
    it('Should render resource global tag with del control', () => {
        const wrapper = mount(
            <AbcResourceTag
                role="del"
                tag={{ name: { ru: 'tag_name' } }}
                onClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should call props.onClick on cross click', () => {
        const onClick = jest.fn();
        const tag = { name: { ru: 'tag_name' } };

        const wrapper = mount(
            <AbcResourceTag
                role="del"
                tag={tag}
                onClick={onClick}
            />
        );

        wrapper.find('.abc-resource-tag__del').simulate('click');
        expect(onClick).toHaveBeenCalledWith(tag);

        wrapper.unmount();
    });
});
