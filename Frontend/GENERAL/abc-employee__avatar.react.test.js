import React from 'react';
import { shallow } from 'enzyme';

import AbcEmployee__Avatar from 'b:abc-employee e:avatar';

describe('AbcEmployee__Avatar', () => {
    it('Should render employee avatar with default params', () => {
        const wrapper = shallow(
            <AbcEmployee__Avatar
                login="robot-serptools"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render small employee avatar', () => {
        const wrapper = shallow(
            <AbcEmployee__Avatar
                login="robot-serptools"
                size="s"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render medium employee avatar', () => {
        const wrapper = shallow(
            <AbcEmployee__Avatar
                login="robot-serptools"
                size="m"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render large employee avatar', () => {
        const wrapper = shallow(
            <AbcEmployee__Avatar
                login="robot-serptools"
                size="l"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
