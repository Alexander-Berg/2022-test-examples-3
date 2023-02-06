import VersionHeader from '../../../../src/components/version-header';
import React from 'react';
import { mount } from 'enzyme';
import initStore from 'store';
import { Provider } from 'react-redux';

beforeAll(() => {
    global.LANG = 'ru';
});

const getStore = (docState, versionDate) => initStore({
    doc: {
        state: docState,
        versionDate
    }
});

const getComponent = (store) => mount(
    <Provider store={store}>
        <VersionHeader/>
    </Provider>
);

let component;
afterEach(() => {
    component.unmount();
});

describe('components/version-header', () => {
    it('should not render if no versionDate', () => {
        component = getComponent(getStore('READY'));
        expect(component.render()).toMatchSnapshot();
    });
    it('should not render if still converting', () => {
        component = getComponent(getStore('WAIT', 1556109733000));
        expect(component.render()).toMatchSnapshot();
    });
    it('should not render if error', () => {
        component = getComponent(getStore('FAIL', 1556109733000));
        expect(component.render()).toMatchSnapshot();
    });
    it('should render if READY and has versionDate', () => {
        component = getComponent(getStore('READY', 1556109733000));
        expect(component.render()).toMatchSnapshot();
    });
    it('should render if ARCHIVE and has versionDate', () => {
        component = getComponent(getStore('ARCHIVE', 1556109733000));
        expect(component.render()).toMatchSnapshot();
    });
});
