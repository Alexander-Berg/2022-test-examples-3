import React from 'react';
import { shallow } from 'enzyme';

import Menu from 'b:menu m:theme=normal m:width=limited';

describe('Menu', () => {
    it('Should render menu with default limited width', () => {
        const wrapper = shallow(
            <Menu
                theme="normal"
                width="limited"
            />
        );
        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render menu with custom limited width', () => {
        const wrapper = shallow(
            <Menu
                theme="normal"
                width="limited"
                maxWidth={42}
            />
        );
        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should keep existing inline styles', () => {
        const wrapper = shallow(
            <Menu
                theme="normal"
                style={{
                    background: 'red'
                }}
                width="limited"
                maxWidth={42}
            />
        );
        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
