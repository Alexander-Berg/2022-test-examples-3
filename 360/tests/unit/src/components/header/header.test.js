import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import Header from '../../../../../src/components/header';
import InlineServicesMenu from '@ps-int/ufo-rocks/lib/components/inline-services-menu';
import getStore from '../../../../../src/store';

jest.mock('../../../../../src/components/user', () => () => null);

const getState = ({ broMode }) => ({
    user: {
        hasDirect: false,
        hasMetrika: false
    },
    environment: {
        nonce: '123',
        tld: 'ru',
        broMode
    },
    services: {}
});
const getComponent = ({
    broMode = false
}) => (
    <Provider store={getStore(getState({ broMode }))}>
        <Header />
    </Provider>
);

describe('components/header =>', () => {
    it('render in bro-mode (i.e. in desktop Yandex.Browser`s pop-up)', () => {
        const wrapper = mount(getComponent({ broMode: true }));

        expect(wrapper.render()).toMatchSnapshot();
    });

    it('render in desktop', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.render()).toMatchSnapshot();
    });

    it('render on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({}));

        expect(wrapper.find(InlineServicesMenu).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });
});
