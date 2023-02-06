import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTagsInput from 'b:abc-resource-tags-input';

describe('AbcResourceTagsInput', () => {
    it('Should render resource tags input', () => {
        const wrapper = mount(
            <AbcResourceTagsInput
                text="text"
                tags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    },
                    {
                        name: { ru: 'tag_name2' },
                        category: { name: { ru: 'category_name2' }, slug: 'category_slug2' },
                        slug: 'tag_slug2'
                    },
                    {
                        name: { ru: 'tag_name3' },
                        category: { name: { ru: 'category_name2' }, slug: 'category_slug2' },
                        slug: 'tag_slug3'
                    }
                ]}
                onClick={jest.fn()}
                onTextChange={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should call props.onClick on click', () => {
        const onClick = jest.fn();

        const wrapper = mount(
            <AbcResourceTagsInput
                text="text"
                tags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    }
                ]}
                onClick={onClick}
                onTextChange={jest.fn()}
            />
        );

        wrapper.find('.abc-resource-tag__add').simulate('click');
        expect(onClick).toHaveBeenCalled();

        wrapper.unmount();
    });

    it('Should call props.onTextChange on test change', () => {
        const onTextChange = jest.fn();

        const wrapper = mount(
            <AbcResourceTagsInput
                text="text"
                tags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    }
                ]}
                onClick={jest.fn()}
                onTextChange={onTextChange}
            />
        );

        wrapper.find('.textinput__control').simulate('change');
        expect(onTextChange).toHaveBeenCalled();

        wrapper.unmount();
    });
});
