import React from 'react';
import { mount } from 'enzyme';

import AbcText from 'b:abc-text m:muted=yes';

describe('AbcText', () => {
    it('Should render a text paragraph', () => {
        const wrapper = mount(
            <AbcText>Hello world!</AbcText>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render a muted text paragraph', () => {
        const wrapper = mount(
            <AbcText muted="yes">Hello world!</AbcText>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
