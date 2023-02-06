import { shallow } from 'enzyme';
import * as React from 'react';
import { ItemStyle } from 'react-tiny-virtual-list';

import { NChats } from '../types';
import { FeedListItem } from './component';
import ITopicMessageItem = NChats.ITopicMessageItem;
import IChatListItem = NChats.IChatListItem;

const ITEM = {
    id: '746009fc-f928-416c-a345-772f755e7d2e',
    last_message: {
        timestamp: 1600348110450,
    } as ITopicMessageItem,
    originator: 'b8c47d81-b7bb-4104-81fa-c302cd32407b',
    topic_link: 'outgoing_communication.746009fc-f928-416c-a345-772f755e7d2e',
    tag_data: {
        chat_meta: {},
        tag_id: 'sd',
    },
    tag_id: '',
    tag_performer_id: '',
    name: '',
    icon: '',
    stats: {
        total: 2,
        unread: 2,
    },
} as IChatListItem;

const USERS = {
    'b8c47d81-b7bb-4104-81fa-c302cd32407b': {
        first_name: 'Анастасия',
        id: 'b8c47d81-b7bb-4104-81fa-c302cd32407b',
        last_name: 'Кулинчик',
        pn: 'Игоревна',
        setup: {},
        username: 'a-kulinchik',
    },
};

const STYLE = {} as ItemStyle;

describe('Feed List Item Tests', () => {
    it('Originator last message', () => {
        const component = shallow(
            <FeedListItem item={ITEM} style={STYLE} users={USERS}/>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Selected item snapshot', () => {
        const component = shallow(
            <FeedListItem item={ITEM} style={STYLE} users={USERS} selectedChatItem={ITEM}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Without stats', () => {
        const item = Object.assign({}, ITEM);
        item.stats = null;

        const component = shallow(
            <FeedListItem item={item} style={STYLE} users={USERS}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Not selected item snapshot', () => {
        const selectedItem = Object.assign({}, ITEM);
        selectedItem.originator = '12345678';

        const component = shallow(
            <FeedListItem item={ITEM} style={STYLE} users={USERS} selectedChatItem={selectedItem}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Not originator last message', () => {
        const component = shallow(
            <FeedListItem item={ITEM} style={STYLE} users={USERS}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Without performer', () => {
        const component = shallow(
            <FeedListItem item={ITEM} style={STYLE} users={USERS}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('With performer', () => {
        const item = Object.assign({}, ITEM);
        item.tag_data?.performer = '123456789';

        const component = shallow(
            <FeedListItem item={ITEM} style={STYLE} users={USERS}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Important item', () => {
        const item = Object.assign({}, ITEM);
        item.tag_data.chat_meta.important = true;

        const component = shallow(
            <FeedListItem item={item} style={STYLE} users={USERS}/>,
        );

        expect(component).toMatchSnapshot();
    });

    it('Not important item', () => {
        const item = Object.assign({}, ITEM);
        item.tag_data.chat_meta.important = false;

        const component = shallow(
            <FeedListItem item={item} style={STYLE} users={USERS}/>,
        );

        expect(component).toMatchSnapshot();
    });
});
