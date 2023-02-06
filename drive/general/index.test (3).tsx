import { shallow } from 'enzyme';
import * as React from 'react';

import { UserCardLinks } from "./UserCardLinks";

describe('Chat Info Tests', () => {
    it('Should render UserCardLinks', () => {
        const component = shallow(
            <UserCardLinks userId='70cea50e-e079-45c3-996c-d65bcb8e7dd2' title='Карточка пользователя' />,
        );

        expect(component).toMatchSnapshot();
    });

    it('Should render UserCardLinks with empty title', () => {
        const component = shallow(
            <UserCardLinks userId='70cea50e-e079-45c3-996c-d65bcb8e7dd2' title='' />,
        );

        expect(component).toMatchSnapshot();
    });

    it('Should render UserCardLinks with empty userId', () => {
        const component = shallow(
            <UserCardLinks userId='' title='Карточка пользователя' />,
        );

        expect(component).toMatchSnapshot();
    });
});
