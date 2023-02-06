import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTag from 'b:abc-resource-tag';

describe('AbcResourceTag', () => {
    it('Should render resource global tag', () => {
        const wrapper = mount(
            <AbcResourceTag
                tag={{ name: { ru: 'tag_name' } }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource service tag', () => {
        const wrapper = mount(
            <AbcResourceTag
                tag={{ name: { ru: 'tag_name' }, service: true }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
