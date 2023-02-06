import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTags from 'b:abc-resource-tags m:role=del';

describe('AbcResourceTags', () => {
    it('Should render resource tags with del controls', () => {
        const wrapper = mount(
            <AbcResourceTags
                role="del"
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
});
