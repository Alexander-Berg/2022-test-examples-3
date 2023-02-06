import React from 'react';
import { shallow } from 'enzyme';

import Message from '../../../../src/components/notification/message';

describe('Message', () => {
    describe('plain', () => {
        test('should render simple text', () => {
            const component = shallow(<Message message={['simple', 'text']} />);

            expect(component.html()).toMatchSnapshot();
        });

        test('should render text with links', () => {
            const component = shallow(
                <Message message={[{ type: 'link', text: 'Bold' }, 'text']} />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render text with resources', () => {
            const component = shallow(
                <Message message={[{ type: 'resource', text: 'Bold' }, 'text']} />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render text with user link', () => {
            const component = shallow(
                <Message
                    message={[{ type: 'user', text: 'Harold' }, 'text']}
                    meta={{ actor: { link: '//yandex.ru' } }} />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render text with empty type', () => {
            const component = shallow(
                <Message message={[{ type: '', text: 'Harold' }, 'text']} />
            );

            expect(component.html()).toMatchSnapshot();
        });
    });
});
