import React from 'react';
import { mount } from 'enzyme';
import inherit from 'inherit';

import AbcResourceTags from 'b:abc-resource-tags';

inherit.self(AbcResourceTags, {}, {
    getScope() {
        if (this.__base(...arguments)) {
            return this.__base(...arguments);
        }
        return document.body;
    }
});

describe('AbcResourceTags', () => {
    it('Should render resource tags', () => {
        const wrapper = mount(
            <AbcResourceTags
                tags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    }
                ]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render empty resource tags', () => {
        const wrapper = mount(
            <AbcResourceTags
                tags={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource tags with popup', () => {
        const wrapper = mount(
            <AbcResourceTags
                tags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    },
                    {
                        name: { ru: 'tag_name2' },
                        slug: 'tag_slug2'
                    }
                ]}
                maxCount={1}
                popupOpen={false}
                onButtonClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource tags with opened popup', () => {
        const wrapper = mount(
            <AbcResourceTags
                tags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    },
                    {
                        name: { ru: 'tag_name2' },
                        slug: 'tag_slug2'
                    }
                ]}
                maxCount={1}
                popupOpen
                onButtonClick={jest.fn()}
                onPopupOutsideClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
