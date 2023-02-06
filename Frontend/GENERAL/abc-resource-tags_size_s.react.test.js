import React from 'react';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import AbcResourceTags from 'b:abc-resource-tags m:size=s';

configure({ adapter: new Adapter() });

describe('AbcResourceTags', () => {
    it('Should render small resource tags', () => {
        const wrapper = mount(
            <AbcResourceTags
                size="s"
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
