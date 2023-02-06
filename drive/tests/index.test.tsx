import { render } from 'enzyme';
import React from 'react';

import { ActiveChatsCounter, IGNORE_CHAT_TAGS } from '../component';

const tags = [
    { tag: IGNORE_CHAT_TAGS[0], topic_link: '123' },
    { tag: 'some_tag_name' },
    { tag: 'some_tag_name', topic_link: '123' },
];
const user_id = '123';

describe('Show active chats count ', () => {
    it('with count', () => {
        const wrapper = render(<ActiveChatsCounter user_id={user_id} tags={[...tags]} isLoading={false}/>);
        expect(wrapper).toMatchSnapshot();
    });
    it('without count', () => {
        const wrapper = render(<ActiveChatsCounter user_id={user_id} tags={[...tags.slice(0, 1)]} isLoading={false}/>);
        expect(wrapper).toMatchSnapshot();
    });
    it('disabled btn', () => {
        const wrapper = render(<ActiveChatsCounter user_id={user_id} tags={[...tags]} disabled/>);
        expect(wrapper).toMatchSnapshot();
    });
});
