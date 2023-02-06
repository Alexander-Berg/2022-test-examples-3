import React from 'react';
import { mount } from 'enzyme';
import { Cut } from '~/src/common/components/Cut/Cut';

describe('Cut', () => {
    it('Should match snapshot', () => {
        const wrapper = mount(
            <Cut hash={1}>
                <div style={{ height: '300px' }} />
            </Cut>
        );

        expect(wrapper).toMatchSnapshot();
    });
});
