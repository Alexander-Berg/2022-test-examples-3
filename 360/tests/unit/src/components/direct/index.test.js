import DirectComponent from '../../../../../src/components/direct';
import { RESOURCE_VIEWS } from '@ps-int/ufo-rocks/lib/consts';
import { Provider } from 'react-redux';
import getStore from '../../store';

import React from 'react';
import { render, mount } from 'enzyme';

const store = getStore({
    url: {
        query: {}
    },
    environment: {
        nonce: 'nonce',
        experiments: {
            flags: {}
        }
    }
});

const Direct = (props) => (
    <Provider store={store}>
        <DirectComponent {...props} />
    </Provider>

);

describe('direct (серверный render) =>', () => {
    it('дефолтное состояние', () => {
        const component = render(
            <Direct/>
        );
        expect(component).toMatchSnapshot();
    });

    it('с параметрами', () => {
        const component = render(
            <Direct
                position="top"
                platform="desktop"
                view={RESOURCE_VIEWS.IMAGE}
            />
        );
        expect(component).toMatchSnapshot();
    });
});

describe('direct (монтирование) =>', () => {
    it('дефолтное состояние', () => {
        const component = mount(
            <Direct/>
        );
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });

    it('с параметрами', () => {
        const component = mount(
            <Direct
                position="top"
                platform="desktop"
                view={RESOURCE_VIEWS.IMAGE}
            />
        );
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });

    it('правый Директ', () => {
        const component = mount(
            <Direct
                position="right"
                platform="desktop"
                view={RESOURCE_VIEWS.DIR}
            />
        );
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });
});
