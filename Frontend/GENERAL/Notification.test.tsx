import React from 'react';

import { shallow } from 'enzyme';
import { Notification } from './Notification/Notification';
import { NotificationIcon } from './NotificationIcon/NotificationIcon';
import { Snackbar } from './Snackbar/Snackbar';
import { IconEdit } from '../../../../static/icons/edit';

describe('UI/Notification/NotificationIcon', () => {
    it('should render NotificationIcon with specified icon', () => {
        const wrapper = shallow(<NotificationIcon icon={<IconEdit />} theme="info" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render NotificationIcon with danger theme', () => {
        const wrapper = shallow(<NotificationIcon theme="danger" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render NotificationIcon with info theme', () => {
        const wrapper = shallow(<NotificationIcon theme="info" />);
        expect(wrapper).toMatchSnapshot();
    });
});

describe('UI/Notification/Notification', () => {
    it('should render default Notification', () => {
        const wrapper = shallow(<Notification theme="info" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Notification with title and content', () => {
        const wrapper = shallow(<Notification title="Title" content="Content" theme="info" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Notification with primary and secondary content', () => {
        const wrapper = shallow(<Notification primaryElement={<span>primary</span>} secondaryElement={<span>secondary</span>} theme="info" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render not closable Notification', () => {
        const wrapper = shallow(<Notification theme="danger" closable={false} />);
        expect(wrapper).toMatchSnapshot();
    });
});

describe('UI/Notification/Snackbar', () => {
    it('should render Snackbar with icon', () => {
        const wrapper = shallow(<Snackbar theme="info" icon={<IconEdit />} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Snackbar with danger theme', () => {
        const wrapper = shallow(<Snackbar theme="danger" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render Snackbar with content', () => {
        const wrapper = shallow(<Snackbar theme="success" content="Content" />);
        expect(wrapper).toMatchSnapshot();
    });
});
