import React from 'react';
import { mount } from 'enzyme';

import { MessageBar as MessageBarDesktop } from './MessageBar.bundle/desktop';
import { MessageBar as MessageBarTouchPad } from './MessageBar.bundle/touch-pad';
import { MessageBar as MessageBarTouchPhone } from './MessageBar.bundle/touch-phone';

const platforms = [
    ['desktop', MessageBarDesktop],
    ['touch-pad', MessageBarTouchPad],
    ['touch-phone', MessageBarTouchPhone]
];

describe.each(platforms)('MessageBar@%s', (_platform, MessageBar) => {
    it('Should render with default props', () => {
        const wrapper = mount(<MessageBar />);

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should render close element', () => {
        const wrapper = mount(
            <MessageBar
                onClose={jest.fn()}
            />,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should not render icon', () => {
        const wrapper = mount(
            <MessageBar
                hasIcon={false}
            />,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should handle close', () => {
        const handleClick = jest.fn();

        const wrapper = mount(
            <MessageBar
                onClose={handleClick}
            />,
        );

        wrapper.find('span.MessageBar-Close').simulate('click');

        expect(handleClick).toHaveBeenCalled();

        wrapper.unmount();
    });
});
