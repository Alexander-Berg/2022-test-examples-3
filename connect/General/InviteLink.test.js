import React from 'react';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';
import InviteLink from './InviteLink';

configure({ adapter: new Adapter() });

describe('client/components/User/Forms/Components/InviteLink', () => {
    it('Should render invite link', () => {
        const wrapper = mount(
            <InviteLink
                inviteLink="https://link.to"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should copy on link click', () => {
        document.execCommand = jest.fn();
        document.queryCommandSupported = command => command === 'copy';

        const wrapper = mount(
            <InviteLink
                inviteLink="https://link.to"
            />
        );

        wrapper.find('.ui-icon__copy').simulate('click');
        expect(document.execCommand).toHaveBeenCalledWith('copy');

        wrapper.unmount();
    });

    it('Should not copy if copy not supported', () => {
        document.execCommand = jest.fn();
        document.queryCommandSupported = () => false;

        const wrapper = mount(
            <InviteLink
                inviteLink="https://link.to"
            />
        );

        wrapper.find('.ui-icon__copy').simulate('click');
        expect(document.execCommand).not.toHaveBeenCalledWith('copy');

        wrapper.unmount();
    });
});
