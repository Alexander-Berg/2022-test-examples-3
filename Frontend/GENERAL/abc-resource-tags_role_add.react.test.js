import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTags from 'b:abc-resource-tags m:role=add';

describe('AbcResourceTags', () => {
    it('Should render resource tags with add controls', () => {
        const wrapper = mount(
            <AbcResourceTags
                role="add"
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
