import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTagsEditor from 'b:abc-resource-tags-editor';

describe('AbcResourceTagsEditor', () => {
    it('Should render resource tags editor', () => {
        const wrapper = mount(
            <AbcResourceTagsEditor
                resourceServiceTags={[
                    {
                        name: { ru: 'tag_name' },
                        slug: 'tag_slug'
                    }
                ]}
                resourceTags={[
                    {
                        name: { ru: 'tag_name2' },
                        slug: 'tag_slug2'
                    }
                ]}
                onItemDelete={jest.fn()}
                onItemAdd={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                onResourceTagsEditCancel={jest.fn()}
                onTextChange={jest.fn()}
                text="text"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource tags editor with error', () => {
        const error = new Error();

        error.data = {
            message: {
                ru: 'Текст ru message',
                en: 'Текст en message'
            }
        };

        const wrapper = mount(
            <AbcResourceTagsEditor
                resourceServiceTags={[]}
                resourceTags={[]}
                onItemDelete={jest.fn()}
                onItemAdd={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                onResourceTagsEditCancel={jest.fn()}
                onTextChange={jest.fn()}
                text="text"
                error={error}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource tags editor with loading', () => {
        const wrapper = mount(
            <AbcResourceTagsEditor
                resourceServiceTags={[]}
                resourceTags={[]}
                onItemDelete={jest.fn()}
                onItemAdd={jest.fn()}
                onResourceTagsEditSubmit={jest.fn()}
                onResourceTagsEditCancel={jest.fn()}
                onTextChange={jest.fn()}
                text="text"
                loading
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
