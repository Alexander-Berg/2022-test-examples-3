import React from 'react';
import { shallow } from 'enzyme';

import ExpandButton from '../../../src/components/expand-button';

describe('Expand button', () => {
    test('simple', () => {
        const component = shallow(
            <ExpandButton text="Hello" icon="//yandex.ru" />
        );

        expect(component.html()).toMatchSnapshot();
    });

    test('with hasUnread', () => {
        const component = shallow(
            <ExpandButton text="Hello" icon="//yandex.ru" hasUnread />
        );

        expect(component.html()).toMatchSnapshot();
    });

    test('with icon', () => {
        const component = shallow(
            <ExpandButton text="Hello" icon={ <div className="custom-icon" /> } />
        );

        expect(component.html()).toMatchSnapshot();
    });
});
